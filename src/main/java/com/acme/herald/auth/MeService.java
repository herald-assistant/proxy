package com.acme.herald.auth;

import com.acme.herald.config.JiraConfigService;
import com.acme.herald.config.JiraProperties;
import com.acme.herald.domain.JiraModels;
import com.acme.herald.domain.dto.MeContextDtos;
import com.acme.herald.provider.JiraProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Service
@RequiredArgsConstructor
public class MeService {

    private static final String PROFILE_PREFIX = "herald.user-profile.v1.";
    private static final JsonNodeFactory NF = JsonNodeFactory.instance;

    private static final String COPILOT_TOKEN_ENC_KEY = "githubCopilotTokenEnc";

    private final JiraProvider jira;
    private final JiraProperties jiraProps;
    private final JiraConfigService jiraConfigService;
    private final CryptoService crypto;

    public MeContextDtos.MeContext context() {
        var user = jira.getMe();
        var perms = jira.getMyPermissions(jiraProps.getProjectKey(), null, null);

        var profile = loadProfilePrefs(user);

        return new MeContextDtos.MeContext(
                user,
                jiraProps.getProjectKey(),
                perms.permissions(),
                profile
        );
    }

    public MeContextDtos.UserProfilePrefs myProfile() {
        var user = jira.getMe();
        return loadProfilePrefs(user);
    }

    public MeContextDtos.UserProfilePrefs updateProfile(@Valid MeContextDtos.UpdateUserProfilePrefs req) {
        var user = jira.getMe();
        var prefsIssueKey = requireUserPrefsIssueKey();

        var propKey = profilePropertyKey(user);
        var now = Instant.now().toString();

        JsonNode existingRaw = jira.getIssueProperty(prefsIssueKey, propKey);
        JsonNode existing = unwrapJiraPropertyValue(existingRaw);

        String existingTokenEnc = strOrNull(existing.get(COPILOT_TOKEN_ENC_KEY));

        // ── token logic ─────────────────────────────
        String newTokenEnc = existingTokenEnc;

        // clear wins
        if (req.clearGithubCopilotToken()) {
            newTokenEnc = null;
        } else {
            String incomingToken = req.githubCopilotToken();
            if (incomingToken != null && !incomingToken.trim().isEmpty()) {
                newTokenEnc = crypto.encrypt(incomingToken.trim().getBytes(StandardCharsets.UTF_8));
            }
        }

        // ── build value ─────────────────────────────
        var value = new HashMap<String, Object>();
        value.put("version", 1);
        value.put("updatedAt", now);
        value.put("userKey", safeUserKey(user));
        value.put("explainUserDescription", req.explainUserDescription());
        value.put("notifyNewTemplates", req.notifyNewTemplates());

        if (newTokenEnc != null && !newTokenEnc.isBlank()) {
            value.put(COPILOT_TOKEN_ENC_KEY, newTokenEnc);
        }

        jira.setIssueProperty(prefsIssueKey, propKey, value);

        return new MeContextDtos.UserProfilePrefs(
                req.explainUserDescription(),
                req.notifyNewTemplates(),
                now,
                (newTokenEnc != null && !newTokenEnc.isBlank())
        );
    }

    // ─────────────────────────────────────────────

    public String getMyGithubCopilotTokenOrNull() {
        var user = jira.getMe();
        return getGithubCopilotTokenOrNull(user);
    }

    public String getGithubCopilotTokenOrNull(JiraModels.UserResponse user) {
        var prefsIssueKey = getUserPrefsIssueKey();
        if (!isNotBlank(prefsIssueKey)) return null;

        var propKey = profilePropertyKey(user);
        JsonNode raw = jira.getIssueProperty(prefsIssueKey, propKey);
        JsonNode v = unwrapJiraPropertyValue(raw);

        String enc = strOrNull(v.get(COPILOT_TOKEN_ENC_KEY));
        if (enc == null) return null;

        byte[] plain = crypto.decrypt(enc);
        String token = new String(plain, StandardCharsets.UTF_8).trim();
        return token.isEmpty() ? null : token;
    }

    private MeContextDtos.UserProfilePrefs loadProfilePrefs(JiraModels.UserResponse user) {
        var prefsIssueKey = getUserPrefsIssueKey();
        var propKey = profilePropertyKey(user);

        if (!isNotBlank(prefsIssueKey)) {
            return new MeContextDtos.UserProfilePrefs("", false, null, false);
        }

        JsonNode raw = jira.getIssueProperty(prefsIssueKey, propKey);
        JsonNode v = unwrapJiraPropertyValue(raw);

        if (v == null || v.isMissingNode() || v.isNull() || (v.isObject() && v.size() == 0)) {
            return new MeContextDtos.UserProfilePrefs("", false, null, false);
        }

        String desc = v.path("explainUserDescription").asString("");
        boolean notify = bool(v.get("notifyNewTemplates"));
        String updatedAt = strOrNull(v.get("updatedAt"));

        boolean tokenPresent = strOrNull(v.get(COPILOT_TOKEN_ENC_KEY)) != null;

        return new MeContextDtos.UserProfilePrefs(desc, notify, updatedAt, tokenPresent);
    }

    private static JsonNode unwrapJiraPropertyValue(JsonNode raw) {
        if (raw == null || raw.isMissingNode() || raw.isNull()) return NF.objectNode();

        JsonNode value = raw.get("value");
        if (value != null && !value.isNull() && !value.isMissingNode()) {
            return value;
        }
        return raw;
    }

    private String requireUserPrefsIssueKey() {
        String issueKey = getUserPrefsIssueKey();
        if (issueKey == null || issueKey.isBlank()) {
            throw new IllegalStateException("Missing userPrefsIssueKey in admin configuration.");
        }
        return issueKey.trim();
    }

    private String getUserPrefsIssueKey() {
        var cfg = jiraConfigService.getForRuntime();
        return (cfg != null) ? cfg.userPrefsIssueKey() : null;
    }

    private String profilePropertyKey(JiraModels.UserResponse user) {
        String raw = safeUserKey(user);
        String b64 = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
        return PROFILE_PREFIX + b64;
    }

    private String safeUserKey(JiraModels.UserResponse user) {
        if (user == null) return "unknown";
        if (user.key() != null && !user.key().isBlank()) return user.key();
        if (user.name() != null && !user.name().isBlank()) return user.name();
        return user.displayName() != null ? user.displayName() : "unknown";
    }

    private static String strOrNull(JsonNode n) {
        if (n == null || n.isNull() || n.isMissingNode()) return null;
        String s = n.asString(null);
        if (s == null) return null;
        s = s.trim();
        return s.isEmpty() ? null : s;
    }

    private static boolean bool(JsonNode n) {
        if (n == null || n.isNull() || n.isMissingNode()) return false;
        if (n.isBoolean()) return n.booleanValue();

        String s = n.asString("").trim().toLowerCase();
        return s.equals("true") || s.equals("1") || s.equals("yes") || s.equals("y") || s.equals("on");
    }
}
