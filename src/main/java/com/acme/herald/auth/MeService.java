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
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Service
@RequiredArgsConstructor
public class MeService {

    private static final String PROFILE_PREFIX = "herald.user-profile.v1.";
    private static final JsonNodeFactory NF = JsonNodeFactory.instance;

    private final JiraProvider jira;
    private final JiraProperties jiraProps;
    private final JiraConfigService jiraConfigService;

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

        // request do Jiry: spokojnie może zostać Mapą (serialize -> JSON)
        var value = Map.<String, Object>of(
                "version", 1,
                "updatedAt", now,
                "userKey", safeUserKey(user),
                "explainUserDescription", req.explainUserDescription(),
                "notifyNewTemplates", req.notifyNewTemplates()
        );

        jira.setIssueProperty(prefsIssueKey, propKey, value);

        return new MeContextDtos.UserProfilePrefs(
                req.explainUserDescription(),
                req.notifyNewTemplates(),
                now
        );
    }

    // ─────────────────────────────────────────────

    private MeContextDtos.UserProfilePrefs loadProfilePrefs(JiraModels.UserResponse user) {
        var prefsIssueKey = getUserPrefsIssueKey();
        var propKey = profilePropertyKey(user);

        if (!isNotBlank(prefsIssueKey)) {
            return new MeContextDtos.UserProfilePrefs("", false, null);
        }

        JsonNode v = jira.getIssueProperty(prefsIssueKey, propKey);

        if (v == null || v.isMissingNode() || v.isNull() || (v.isObject() && v.size() == 0)) {
            return new MeContextDtos.UserProfilePrefs("", false, null);
        }

        String desc = v.path("explainUserDescription").asString("");
        boolean notify = bool(v.get("notifyNewTemplates"));
        String updatedAt = strOrNull(v.get("updatedAt"));

        return new MeContextDtos.UserProfilePrefs(desc, notify, updatedAt);
    }

    /**
     * Jira issue property response zwykle: { "key": "...", "value": { ... } }
     * Ale czasem możesz dostać już samą wartość.
     */
    private static JsonNode unwrapJiraPropertyValue(JsonNode raw) {
        if (raw == null || raw.isMissingNode() || raw.isNull()) return NF.objectNode();

        JsonNode value = raw.get("value");
        if (value != null && !value.isNull() && !value.isMissingNode()) {
            return value;
        }

        // fallback: traktuj raw jako wartość
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

        // Jira property bywa stringiem
        String s = n.asString("").trim().toLowerCase();
        return s.equals("true") || s.equals("1") || s.equals("yes") || s.equals("y") || s.equals("on");
    }
}
