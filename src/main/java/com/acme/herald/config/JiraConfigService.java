package com.acme.herald.config;

import com.acme.herald.domain.JiraModels;
import com.acme.herald.provider.JiraProvider;
import com.acme.herald.web.error.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static com.acme.herald.config.JiraIntegrationConfigDtos.*;

@Service
@RequiredArgsConstructor
public class JiraConfigService {
    private static final Pattern HEX_COLOR = Pattern.compile("^#([0-9a-fA-F]{6}|[0-9a-fA-F]{3})$");
    private static final int BANNER_TEXT_MAX = 160;
    private static final String DEFAULT_BANNER_COLOR = "#ff897d";

    private static final String PROP_KEY = "herald.jiraConfig";
    private static final String PERM_ADMIN = "ADMINISTER_PROJECTS";

    private static final Set<String> CASE_ALLOWED = Set.of("TODO", "IN_PROGRESS", "DONE", "IN_REVIEW", "REJECTED");
    private static final List<String> CASE_REQUIRED = List.of("TODO", "IN_PROGRESS", "DONE", "REJECTED");

    private static final Set<String> TEMPLATE_ALLOWED = Set.of("TODO", "IN_PROGRESS", "DONE", "IN_REVIEW", "PUBLISHED", "REJECTED", "DEPRECATED");
    private static final List<String> TEMPLATE_REQUIRED = List.of("TODO", "IN_PROGRESS", "DONE", "PUBLISHED", "REJECTED", "DEPRECATED");

    private final JiraProvider jira;
    private final JiraProperties jiraProps;
    private final JsonMapper jsonMapper;

    // ─────────── ADMIN endpoints ───────────

    public void saveForAdmin(JiraIntegrationConfigDto incoming) {
        requireProjectAdmin();

        StoredJiraIntegration out = new StoredJiraIntegration(
                incoming.version() != null ? incoming.version() : 1,
                nz(incoming.baseUrl()),
                nz(incoming.projectKey()),
                normalizeIssueTypes(incoming.issueTypes()),
                normalizeFields(incoming.fields()),
                normalizeLinks(incoming.links()),
                normalizeOptions(incoming.options()),
                normalizeStatus(incoming.status()),
                normalizeAccess(incoming.access()),
                nz(incoming.userPrefsIssueKey()),
                normalizeBanner(incoming.banner())
        );

        validateOrThrow(out);

        saveStored(out);
    }

    public List<String> groupPickerForAdmin(String query, int limit) {
        requireProjectAdmin();
        return jira.groupPicker(nz(query), List.of(), limit);
    }

    // ─────────── runtime (dla normalnych userów) ───────────

    public JiraIntegrationConfigDto getForRuntime() {
        StoredJiraIntegration stored = loadStoredOrDefault();
        return toDto(stored);
    }

    // ───────────────────────────────────────────────────────

    private void requireProjectAdmin() {
        String storageProjectKey = jiraProps.getProjectKey();
        JiraModels.PermissionsResponse perms = jira.getMyPermissions(storageProjectKey, null, null);
        var map = perms != null ? perms.permissions() : null;

        boolean isAdmin = map != null
                && map.get(PERM_ADMIN) != null
                && Boolean.TRUE.equals(map.get(PERM_ADMIN).havePermission());

        if (!isAdmin) {
            throw new ForbiddenException("Brak uprawnienia: " + PERM_ADMIN + " w projekcie " + storageProjectKey);
        }
    }

    private StoredJiraIntegration loadStoredOrDefault() {
        try {
            JsonNode value = jira.getProjectProperty(jiraProps.getProjectKey(), PROP_KEY);

            if (value == null || value.isNull() || value.isMissingNode()) {
                return defaultConfig();
            }

            StoredJiraIntegration stored = jsonMapper.treeToValue(value, StoredJiraIntegration.class);
            return mergeWithDefaults(stored);
        } catch (Exception e) {
            return defaultConfig();
        }
    }

    private void saveStored(StoredJiraIntegration stored) {
        jira.setProjectProperty(jiraProps.getProjectKey(), PROP_KEY, stored);
    }

    private JiraIntegrationConfigDto toDto(StoredJiraIntegration s) {
        String effectiveProjectKey = pick(s.projectKey(), jiraProps.getProjectKey());
        String effectiveBaseUrl = pick(s.baseUrl(), jiraProps.getBaseUrl());

        return new JiraIntegrationConfigDto(
                s.version(),
                effectiveBaseUrl,
                effectiveProjectKey,
                s.issueTypes(),
                s.fields(),
                s.links(),
                s.options(),
                s.status(),
                s.access(),
                s.userPrefsIssueKey(),
                s.banner()
        );
    }

    // ─────────── Defaults + normalizacja ───────────

    private StoredJiraIntegration defaultConfig() {
        return new StoredJiraIntegration(
                1,
                "",
                "",
                new JiraIssueTypesConfigDto("Epic", "Task", "Story", "Sub-task"),
                new JiraFieldsConfigDto(
                        "summary",      // templateId
                        "summary",      // caseId
                        "description",  // payload
                        "unused",       // casePayload
                        "unused",       // epicLink
                        "unused",       // ratingAvg
                        "unused",  // description
                        "",             // caseStatus (customfield_x)
                        ""              // templateStatus (customfield_x)
                ),
                new JiraLinksConfigDto(null, null),
                new JiraOptionsConfigDto(true, true, false),
                new JiraStatusConfigDto(
                        Map.of(
                                "TODO", "To Do",
                                "IN_PROGRESS", "In Progress",
                                "DONE", "Done",
                                "REJECTED", "Rejected"
                        ),
                        Map.of(
                                "TODO", "To Do",
                                "IN_PROGRESS", "In Progress",
                                "DONE", "Done",
                                "PUBLISHED", "Published",
                                "REJECTED", "Rejected",
                                "DEPRECATED", "Deprecated"
                        )
                ),
                new JiraAccessConfigDto(List.of(), List.of()),
                "", // userPrefsIssueKey,
                new UiBannerDto("", DEFAULT_BANNER_COLOR)
        );
    }

    private StoredJiraIntegration mergeWithDefaults(StoredJiraIntegration s) {
        StoredJiraIntegration d = defaultConfig();
        return new StoredJiraIntegration(
                (s != null && s.version() != null ? s.version() : d.version()),
                pick(s != null ? s.baseUrl() : null, d.baseUrl()),
                pick(s != null ? s.projectKey() : null, d.projectKey()),
                (s != null && s.issueTypes() != null ? s.issueTypes() : d.issueTypes()),
                (s != null && s.fields() != null ? mergeFieldsWithDefaults(s.fields(), d.fields()) : d.fields()),
                (s != null && s.links() != null ? s.links() : d.links()),
                (s != null && s.options() != null ? s.options() : d.options()),
                (s != null && s.status() != null ? mergeStatusWithDefaults(s.status(), d.status()) : d.status()),
                (s != null && s.access() != null ? s.access() : d.access()),
                pick(s != null ? s.userPrefsIssueKey() : null, d.userPrefsIssueKey()),
                (s != null && s.banner() != null ? normalizeBanner(s.banner()) : d.banner()) // NEW
        );
    }

    private JiraFieldsConfigDto mergeFieldsWithDefaults(JiraFieldsConfigDto s, JiraFieldsConfigDto d) {
        return new JiraFieldsConfigDto(
                pick(s.templateId(), d.templateId()),
                pick(s.caseId(), d.caseId()),
                pick(s.payload(), d.payload()),
                pick(s.casePayload(), d.casePayload()),
                pick(s.epicLink(), d.epicLink()),
                pick(s.ratingAvg(), d.ratingAvg()),
                pick(s.description(), d.description()),
                pick(s.caseStatus(), d.caseStatus()),
                pick(s.templateStatus(), d.templateStatus())
        );
    }

    private JiraStatusConfigDto mergeStatusWithDefaults(JiraStatusConfigDto s, JiraStatusConfigDto d) {
        Map<String, String> caseMap = mergeStatusMap(
                s.caseStatusMap(), d.caseStatusMap(),
                CASE_ALLOWED, CASE_REQUIRED
        );

        Map<String, String> templateMap = mergeStatusMap(
                s.templateStatusMap(), d.templateStatusMap(),
                TEMPLATE_ALLOWED, TEMPLATE_REQUIRED
        );

        // IN_REVIEW jest opcjonalne -> jeśli jest ustawione, zostawiamy
        String inReviewCase = normalizeKeyLookup(s.caseStatusMap(), "IN_REVIEW");
        if (inReviewCase != null) {
            caseMap.put("IN_REVIEW", inReviewCase);
        }

        String inReviewTpl = normalizeKeyLookup(s.templateStatusMap(), "IN_REVIEW");
        if (inReviewTpl != null) {
            templateMap.put("IN_REVIEW", inReviewTpl);
        }

        return new JiraStatusConfigDto(caseMap, templateMap);
    }

    private static Map<String, String> mergeStatusMap(
            Map<String, String> src,
            Map<String, String> def,
            Set<String> allowed,
            List<String> required
    ) {
        Map<String, String> out = new LinkedHashMap<>();

        if (src != null) {
            for (var e : src.entrySet()) {
                String k = normalizeKey(e.getKey());
                if (!allowed.contains(k)) continue;
                String v = nz(e.getValue());
                if (!v.isBlank()) out.put(k, v);
            }
        }

        // required keys fallback to defaults
        if (def != null) {
            for (String k : required) {
                if (!out.containsKey(k)) {
                    String dv = nz(def.get(k));
                    if (!dv.isBlank()) out.put(k, dv);
                }
            }
        }

        return out;
    }

    private static String normalizeKeyLookup(Map<String, String> map, String keyUpper) {
        if (map == null) return null;
        for (var e : map.entrySet()) {
            if (normalizeKey(e.getKey()).equals(keyUpper)) {
                String v = nz(e.getValue());
                return v.isBlank() ? null : v;
            }
        }
        return null;
    }

    private static String normalizeKey(String k) {
        return k == null ? "" : k.trim().toUpperCase();
    }

    private static String pick(String value, String def) {
        String t = value == null ? "" : value.trim();
        return t.isEmpty() ? def : t;
    }

    private JiraIssueTypesConfigDto normalizeIssueTypes(JiraIssueTypesConfigDto in) {
        if (in == null) return defaultConfig().issueTypes();
        return new JiraIssueTypesConfigDto(
                nz(in.epic()),
                nz(in.template()),
                nz(in.caseIssue()),
                nz(in.section())
        );
    }

    private JiraFieldsConfigDto normalizeFields(JiraFieldsConfigDto in) {
        if (in == null) return defaultConfig().fields();
        return new JiraFieldsConfigDto(
                nz(in.templateId()),
                nz(in.caseId()),
                nz(in.payload()),
                nz(in.casePayload()),
                nz(in.epicLink()),
                nz(in.ratingAvg()),
                nz(in.description()),
                nz(in.caseStatus()),
                nz(in.templateStatus())
        );
    }

    private JiraLinksConfigDto normalizeLinks(JiraLinksConfigDto in) {
        if (in == null) return defaultConfig().links();
        return new JiraLinksConfigDto(
                nz(in.templateToCase()),
                nz(in.templateToFork())
        );
    }

    private JiraOptionsConfigDto normalizeOptions(JiraOptionsConfigDto in) {
        JiraOptionsConfigDto d = defaultConfig().options();
        if (in == null) return d;
        return new JiraOptionsConfigDto(
                in.proxyAttachmentContent() != null ? in.proxyAttachmentContent() : d.proxyAttachmentContent(),
                in.attachHeaderNoCheck() != null ? in.attachHeaderNoCheck() : d.attachHeaderNoCheck(),
                in.useOptimisticLock() != null ? in.useOptimisticLock() : d.useOptimisticLock()
        );
    }

    private JiraStatusConfigDto normalizeStatus(JiraStatusConfigDto in) {
        JiraStatusConfigDto d = defaultConfig().status();
        if (in == null) return d;

        Map<String, String> caseMap = normalizeStatusMap(
                in.caseStatusMap(),
                d.caseStatusMap(),
                CASE_ALLOWED
        );

        Map<String, String> templateMap = normalizeStatusMap(
                in.templateStatusMap(),
                d.templateStatusMap(),
                TEMPLATE_ALLOWED
        );

        return new JiraStatusConfigDto(caseMap, templateMap);
    }

    private JiraAccessConfigDto normalizeAccess(JiraAccessConfigDto in) {
        if (in == null) return new JiraAccessConfigDto(List.of(), List.of());
        return new JiraAccessConfigDto(
                normalizeGroupList(in.allowGroups()),
                normalizeGroupList(in.denyGroups())
        );
    }

    private static List<String> normalizeGroupList(List<String> in) {
        if (in == null) return List.of();
        return in.stream()
                .map(s -> s == null ? "" : s.trim())
                .filter(s -> !s.isBlank())
                .distinct()
                .limit(200)
                .toList();
    }

    private static Map<String, String> normalizeStatusMap(
            Map<String, String> src,
            Map<String, String> def,
            Set<String> allowed
    ) {
        Map<String, String> out = new LinkedHashMap<>();

        if (src != null) {
            for (var e : src.entrySet()) {
                String k = normalizeKey(e.getKey());
                if (!allowed.contains(k)) continue;
                String v = nz(e.getValue());
                if (!v.isBlank()) out.put(k, v);
            }
        }

        // jeśli po normalizacji nic nie ma -> bierzemy defaulty w całości
        if (out.isEmpty() && def != null && !def.isEmpty()) {
            for (var e : def.entrySet()) {
                String k = normalizeKey(e.getKey());
                if (!allowed.contains(k)) continue;
                String v = nz(e.getValue());
                if (!v.isBlank()) out.put(k, v);
            }
        }

        return out;
    }

    private UiBannerDto normalizeBanner(UiBannerDto in) {
        if (in == null) return new UiBannerDto("", DEFAULT_BANNER_COLOR);

        String text = nz(in.text())
                .replace("\n", " ")
                .replace("\r", " ")
                .trim();

        if (text.length() > BANNER_TEXT_MAX) {
            text = text.substring(0, BANNER_TEXT_MAX).trim();
        }

        String color = nz(in.color()).trim();
        if (color.isBlank()) color = DEFAULT_BANNER_COLOR;
        if (!HEX_COLOR.matcher(color).matches()) color = DEFAULT_BANNER_COLOR;

        return new UiBannerDto(text, color);
    }


    private void validateOrThrow(StoredJiraIntegration cfg) {
        JiraFieldsConfigDto f = cfg.fields();
        JiraStatusConfigDto s = cfg.status();

        boolean missingField = f == null
                || f.caseStatus() == null || f.caseStatus().isBlank()
                || f.templateStatus() == null || f.templateStatus().isBlank();

        boolean missingUserPrefsIssueKey = cfg.userPrefsIssueKey() == null || cfg.userPrefsIssueKey().isBlank();

        if (missingUserPrefsIssueKey) {
            throw new IllegalArgumentException("Jira config: musisz ustawić userPrefsIssueKey (np. HERALD-OPS-1).");
        }
        if (missingField) {
            throw new IllegalArgumentException("Jira config: musisz ustawić fields.caseStatus oraz fields.templateStatus (customfield_xxxxx).");
        }
        if (s == null) {
            throw new IllegalArgumentException("Jira config: musisz ustawić status.caseStatusMap oraz status.templateStatusMap.");
        }

        requireKeys(s.caseStatusMap(), CASE_REQUIRED, "status.caseStatusMap");
        requireKeys(s.templateStatusMap(), TEMPLATE_REQUIRED, "status.templateStatusMap");
    }

    private static void requireKeys(Map<String, String> map, List<String> requiredKeys, String where) {
        if (map == null || map.isEmpty()) {
            throw new IllegalArgumentException("Jira config: " + where + " nie może być puste.");
        }
        for (String k : requiredKeys) {
            String v = map.get(k);
            if (v == null || v.isBlank()) {
                throw new IllegalArgumentException("Jira config: " + where + " musi zawierać klucz " + k + " (niepusty).");
            }
        }
    }

    private static String nz(String s) {
        return s == null ? "" : s.trim();
    }
}
