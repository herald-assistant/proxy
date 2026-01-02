package com.acme.herald.service;

import com.acme.herald.config.JiraProperties;
import com.acme.herald.domain.JiraModels;
import com.acme.herald.provider.JiraProvider;
import com.acme.herald.web.error.ForbiddenException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import static com.acme.herald.web.admin.JiraIntegrationDtos.*;

@Service
@RequiredArgsConstructor
public class AdminJiraConfigService {

    private static final String PROP_KEY = "herald.jiraConfig";
    private static final String PERM_ADMIN = "ADMINISTER_PROJECTS";

    private final JiraProvider jira;
    private final JiraProperties jiraProps;
    private final ObjectMapper om = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private volatile JiraIntegrationDto runtimeCache;

    // ─────────── ADMIN endpoints ───────────

    public JiraIntegrationDto getForAdmin() {
        requireProjectAdmin();
        StoredJiraIntegration stored = loadStoredOrDefault();
        return toDto(stored);
    }

    public void saveForAdmin(JiraIntegrationDto incoming) {
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
                nz(incoming.userPrefsIssueKey())
        );

        validateStatusConfigOrThrow(out);

        saveStored(out);

        runtimeCache = toDto(out);
    }

    // ─────────── runtime (dla normalnych userów) ───────────

    public JiraIntegrationDto getForRuntime() {
        JiraIntegrationDto cached = runtimeCache;
        if (cached != null) return cached;

        try {
            StoredJiraIntegration stored = loadStoredOrDefault();
            JiraIntegrationDto dto = toDto(stored);
            runtimeCache = dto;
            return dto;
        } catch (Exception e) {
            JiraIntegrationDto dto = toDto(defaultConfig());
            runtimeCache = dto;
            return dto;
        }
    }

    // ───────────────────────────────────────────────────────

    private void requireProjectAdmin() {
        String storageProjectKey = jiraProps.getProjectKey(); // tu trzymamy project property (repo config)
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
        Map<String, Object> resp = jira.getProjectProperty(jiraProps.getProjectKey(), PROP_KEY);
        if (resp == null || !resp.containsKey("value") || resp.get("value") == null) {
            return defaultConfig();
        }
        Object value = resp.get("value");
        try {
            StoredJiraIntegration stored = om.convertValue(value, StoredJiraIntegration.class);
            return mergeWithDefaults(stored);
        } catch (Exception e) {
            return defaultConfig();
        }
    }

    private void saveStored(StoredJiraIntegration stored) {
        jira.setProjectProperty(jiraProps.getProjectKey(), PROP_KEY, stored);
    }

    private JiraIntegrationDto toDto(StoredJiraIntegration s) {
        String effectiveProjectKey = pick(s.projectKey(), jiraProps.getProjectKey());
        String effectivBbaseUrl = pick(s.baseUrl(), jiraProps.getBaseUrl());

        return new JiraIntegrationDto(
                s.version(),
                effectivBbaseUrl,
                effectiveProjectKey,
                s.issueTypes(),
                s.fields(),
                s.links(),
                s.options(),
                s.status(),
                s.userPrefsIssueKey()
        );
    }

    // ─────────── Defaults + normalizacja ───────────

    private StoredJiraIntegration defaultConfig() {
        return new StoredJiraIntegration(
                1,
                "",
                "",
                new JiraIssueTypesDto("Epic", "Task", "Story", "Sub-task"),
                new JiraFieldsDto(
                        "summary", // templateId
                        "summary", // caseId
                        "description", // payload
                        "unused", // casePayload
                        "unused", // epicLink
                        "unused", // ratingAvg
                        "unused", // description
                        "", // caseStatus
                        "" // templateStatus
                ),
                new JiraLinksDto("Implements"),
                new JiraOptionsDto(true, true, false),
                new JiraStatusDto(
                        List.of("todo", "in_progress", "done", "rejected"),
                        List.of("todo", "in_progress", "done", "rejected", "published")
                ),
                "" // userPrefsIssueKey
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
                pick(s != null ? s.userPrefsIssueKey() : null, d.userPrefsIssueKey())
        );
    }

    private JiraFieldsDto mergeFieldsWithDefaults(JiraFieldsDto s, JiraFieldsDto d) {
        return new JiraFieldsDto(
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

    private JiraStatusDto mergeStatusWithDefaults(JiraStatusDto s, JiraStatusDto d) {
        return new JiraStatusDto(
                (s.caseFlow() != null && !s.caseFlow().isEmpty()) ? s.caseFlow() : d.caseFlow(),
                (s.templateFlow() != null && !s.templateFlow().isEmpty()) ? s.templateFlow() : d.templateFlow()
        );
    }

    private static String pick(String value, String def) {
        String t = value == null ? "" : value.trim();
        return t.isEmpty() ? def : t;
    }

    private JiraIssueTypesDto normalizeIssueTypes(JiraIssueTypesDto in) {
        if (in == null) return defaultConfig().issueTypes();
        return new JiraIssueTypesDto(
                nz(in.epic()),
                nz(in.template()),
                nz(in.caseIssue()),
                nz(in.section())
        );
    }

    private JiraFieldsDto normalizeFields(JiraFieldsDto in) {
        if (in == null) return defaultConfig().fields();
        return new JiraFieldsDto(
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

    private JiraLinksDto normalizeLinks(JiraLinksDto in) {
        if (in == null) return defaultConfig().links();
        return new JiraLinksDto(nz(in.templateToCase()));
    }

    private JiraOptionsDto normalizeOptions(JiraOptionsDto in) {
        JiraOptionsDto d = defaultConfig().options();
        if (in == null) return d;
        return new JiraOptionsDto(
                in.proxyAttachmentContent() != null ? in.proxyAttachmentContent() : d.proxyAttachmentContent(),
                in.attachHeaderNoCheck() != null ? in.attachHeaderNoCheck() : d.attachHeaderNoCheck(),
                in.useOptimisticLock() != null ? in.useOptimisticLock() : d.useOptimisticLock()
        );
    }

    private JiraStatusDto normalizeStatus(JiraStatusDto in) {
        JiraStatusDto d = defaultConfig().status();
        if (in == null) return d;

        List<String> caseFlow = normalizeFlow(in.caseFlow(), d.caseFlow());
        List<String> templateFlow = normalizeFlow(in.templateFlow(), d.templateFlow());

        return new JiraStatusDto(caseFlow, templateFlow);
    }

    private static List<String> normalizeFlow(List<String> src, List<String> def) {
        if (src == null) return def;
        List<String> out = src.stream()
                .map(AdminJiraConfigService::nz)
                .filter(s -> !s.isBlank())
                .distinct()
                .toList();
        return out.isEmpty() ? def : out;
    }

    private void validateStatusConfigOrThrow(StoredJiraIntegration cfg) {
        JiraFieldsDto f = cfg.fields();
        JiraStatusDto s = cfg.status();

        boolean missingField = f == null
                || f.caseStatus() == null || f.caseStatus().isBlank()
                || f.templateStatus() == null || f.templateStatus().isBlank();

        boolean missingFlow = s == null
                || s.caseFlow() == null || s.caseFlow().isEmpty()
                || s.templateFlow() == null || s.templateFlow().isEmpty();

        if (missingField) {
            throw new IllegalArgumentException("Jira config: musisz ustawić fields.caseStatus oraz fields.templateStatus (customfield_xxxxx).");
        }
        if (missingFlow) {
            throw new IllegalArgumentException("Jira config: musisz ustawić status.caseFlow oraz status.templateFlow (lista statusów).");
        }
    }

    private static String nz(String s) {
        return s == null ? "" : s.trim();
    }
}
