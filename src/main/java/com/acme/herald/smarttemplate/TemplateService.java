package com.acme.herald.smarttemplate;

import com.acme.herald.config.JiraProperties;
import com.acme.herald.domain.dto.CreateTemplate;
import com.acme.herald.domain.dto.TemplateRef;
import com.acme.herald.provider.JiraProvider;
import com.acme.herald.config.AdminJiraConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

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

        if (req.status() != null && cfg.status().templateFlow().stream().noneMatch(s -> s.equals(req.status()))) {
            throw new IllegalArgumentException("Niepoprawny status template: " + req.status() + ". Dozwolone: " + cfg.status().templateFlow());
        }

        // pola issue
        var fields = new java.util.HashMap<String, Object>();
        fields.put("summary", isNotBlank(req.title()) ? req.title() : req.template_id());
        fields.put("project", Map.of("key", jiraProps.getProjectKey()));
        fields.put("issuetype", Map.of("name", issueTypes.template()));
        fields.put("labels", req.labels() != null ? req.labels() : List.of());
        fields.put(fieldsCfg.templateId(), req.template_id());
        fields.put(fieldsCfg.payload(), req.payload().toString());
        fields.put(fieldsCfg.templateStatus(), req.status());

        String jql = "%s ~ \"%s\""
                .formatted(
                        toJqlField(fieldsCfg.templateId()),
                        escapeJql(req.template_id())
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
        return s.replace("\"", "\\\"");
    }
}
