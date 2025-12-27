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
                normalizeIssueTypes(incoming.issueTypes()),
                normalizeFields(incoming.fields()),
                normalizeLinks(incoming.links()),
                normalizeOptions(incoming.options())
        );

        saveStored(out);
    }

    // ─────────── runtime (dla normalnych userów) ───────────
    // Bez sprawdzania admin perms — bo case/template mają działać dla zwykłych użytkowników.

    public JiraIntegrationDto getForRuntime() {
        StoredJiraIntegration stored = loadStoredOrDefault();
        return toDto(stored);
    }

    // ───────────────────────────────────────────────────────

    private void requireProjectAdmin() {
        var projectKey = jiraProps.getProjectKey();
        JiraModels.PermissionsResponse perms = jira.getMyPermissions(projectKey, null, null);
        var map = perms != null ? perms.permissions() : null;

        boolean isAdmin = map != null
                && map.get(PERM_ADMIN) != null
                && Boolean.TRUE.equals(map.get(PERM_ADMIN).havePermission());

        if (!isAdmin) {
            throw new ForbiddenException("Brak uprawnienia: " + PERM_ADMIN + " w projekcie " + projectKey);
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
            // jeśli ktoś zapisał śmieci w property → wracamy do default i nie wysypujemy runtime
            return defaultConfig();
        }
    }

    private void saveStored(StoredJiraIntegration stored) {
        jira.setProjectProperty(jiraProps.getProjectKey(), PROP_KEY, stored);
    }

    private JiraIntegrationDto toDto(StoredJiraIntegration s) {
        return new JiraIntegrationDto(s.version(), s.issueTypes(), s.fields(), s.links(), s.options());
    }

    // ─────────── Defaults + normalizacja ───────────

    private StoredJiraIntegration defaultConfig() {
        return new StoredJiraIntegration(
                1,
                new JiraIssueTypesDto("Epic", "Task", "Story", "Sub-task"),
                new JiraFieldsDto(
                        "customfield_10112", // templateId
                        "customfield_10113", // caseId
                        "customfield_10114", // payload
                        "customfield_10301", // casePayload
                        "customfield_10101", // epicLink
                        "customfield_10115", // ratingAvg
                        "description"        // description
                ),
                new JiraLinksDto("Implements"),
                new JiraOptionsDto(true, true, false)
        );
    }

    private StoredJiraIntegration mergeWithDefaults(StoredJiraIntegration s) {
        StoredJiraIntegration d = defaultConfig();
        return new StoredJiraIntegration(
                (s.version() != null ? s.version() : d.version()),
                (s.issueTypes() != null ? s.issueTypes() : d.issueTypes()),
                (s.fields() != null ? s.fields() : d.fields()),
                (s.links() != null ? s.links() : d.links()),
                (s.options() != null ? s.options() : d.options())
        );
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
                nz(in.description())
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

    private static String nz(String s) {
        return s == null ? "" : s.trim();
    }
}
