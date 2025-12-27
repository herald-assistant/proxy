package com.acme.herald.service;

import com.acme.herald.config.JiraProperties;
import com.acme.herald.domain.JiraModels;
import com.acme.herald.domain.dto.CreateTemplate;
import com.acme.herald.domain.dto.TemplateRef;
import com.acme.herald.provider.JiraProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class TemplateService {

    private final JiraProvider jira;
    private final JiraProperties jiraProps;
    private final AdminJiraConfigService jiraAdminCfg;

    public TemplateRef createTemplate(CreateTemplate req) {
        var cfg = jiraAdminCfg.getForRuntime();
        var issueTypes = cfg.issueTypes();
        var fieldsCfg = cfg.fields();

        var fields = Map.of(
                "project", Map.of("key", jiraProps.getProjectKey()),
                "summary", req.title(),
                "issuetype", Map.of("name", issueTypes.template()),
                fieldsCfg.templateId(), req.template_id(),
                "labels", req.labels() != null ? req.labels() : List.of(),
                fieldsCfg.payload(), req.payload().toString()
        );

        // Zamiast "herald_template_id ~ ..." użyjemy stabilnie customfield id -> cf[10112]
        String jql = "%s ~ \"%s\" AND %s is EMPTY"
                .formatted(
                        toJqlField(fieldsCfg.templateId()),
                        escapeJql(req.template_id()),
                        toJqlField(fieldsCfg.caseId())
                );

        var existing = jira.search(jql, 0, 1);

        String issueKey;
        if (existing.total() == 0) {
            issueKey = jira.createIssue(Map.of("fields", fields)).key();
        } else {
            Map<String, Object> issue = existing.issues().stream().findAny().orElseThrow();
            issueKey = String.valueOf(issue.get("key"));
            jira.updateIssue(issueKey, Map.of("fields", fields));
        }

        String url = jiraProps.getBaseUrl() + "/browse/" + issueKey;
        return new TemplateRef(issueKey, url);
    }

    // Zostawiam (jeśli jeszcze gdzieś używasz); na MVP możesz docelowo wywalić statusy.
    public void transition(String issueKey, String transitionId) {
        jira.transition(issueKey, transitionId);
    }

    public JiraModels.TransitionList transitions(String issueKey) {
        return jira.transitions(issueKey);
    }

    // ───────────────── helpers ─────────────────

    private static final Pattern CUSTOMFIELD = Pattern.compile("^customfield_(\\d+)$");

    /** "customfield_10112" -> "cf[10112]" (bezpieczne w JQL), inne -> zwraca bez zmian */
    static String toJqlField(String fieldIdOrName) {
        String f = fieldIdOrName == null ? "" : fieldIdOrName.trim();
        Matcher m = CUSTOMFIELD.matcher(f);
        if (m.matches()) return "cf[" + m.group(1) + "]";
        return f;
    }

    static String escapeJql(String s) {
        if (s == null) return "";
        // minimalnie: escapuj cudzysłowy
        return s.replace("\"", "\\\"");
    }
}
