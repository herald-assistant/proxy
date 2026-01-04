package com.acme.herald.auth;

import com.acme.herald.config.AdminJiraConfigService;
import com.acme.herald.config.JiraProperties;
import com.acme.herald.domain.JiraModels;
import com.acme.herald.domain.dto.MeContextDtos;
import com.acme.herald.provider.JiraProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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

    private final JiraProvider jira;
    private final JiraProperties jiraProps;
    private final AdminJiraConfigService jiraConfigService;

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
        Map<String, Object> m = new HashMap<>();

        if (isNotBlank(prefsIssueKey)) {
            Map<String, Object> raw = jira.getIssueProperty(prefsIssueKey, propKey);
            m = unwrapJiraPropertyValue(raw);
        }

        if (m.isEmpty()) {
            return new MeContextDtos.UserProfilePrefs("", false, null);
        }

        String desc = str(m.get("explainUserDescription"));
        boolean notify = bool(m.get("notifyNewTemplates"));
        String updatedAt = strOrNull(m.get("updatedAt"));

        return new MeContextDtos.UserProfilePrefs(desc, notify, updatedAt);
    }

    /**
     * Jira issue property response usually: { "key": "...", "value": { ... } }
     * Ale bywa, że dostaniesz już samą wartość (np. mock / inne źródło).
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> unwrapJiraPropertyValue(Map<String, Object> raw) {
        if (raw == null || raw.isEmpty()) return Map.of();

        Object v = raw.get("value");
        if (v instanceof Map<?, ?> vm) {
            return (Map<String, Object>) vm;
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

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    private static String strOrNull(Object o) {
        if (o == null) return null;
        String s = String.valueOf(o).trim();
        return s.isEmpty() ? null : s;
    }

    private static boolean bool(Object o) {
        if (o == null) return false;
        if (o instanceof Boolean b) return b;
        String s = String.valueOf(o).trim().toLowerCase();
        return s.equals("true") || s.equals("1") || s.equals("yes");
    }
}
