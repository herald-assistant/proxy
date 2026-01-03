package com.acme.herald.smartcase;

import com.acme.herald.config.AdminJiraConfigService;
import com.acme.herald.config.JiraProperties;
import com.acme.herald.domain.dto.CaseRef;
import com.acme.herald.domain.dto.CreateCase;
import com.acme.herald.domain.dto.RatingInput;
import com.acme.herald.domain.dto.RatingResult;
import com.acme.herald.provider.JiraProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Service
@RequiredArgsConstructor
public class CaseService {
    private final JiraProvider jira;
    private final JiraProperties jiraProps;
    private final AdminJiraConfigService jiraAdminCfg;

    public CaseRef upsertCase(CreateCase req) {
        var cfg = jiraAdminCfg.getForRuntime();
        var issueTypes = cfg.issueTypes();
        var fieldsCfg = cfg.fields();

        if (req.status() != null && cfg.status().caseFlow().stream().noneMatch(s -> s.equals(req.status()))) {
            throw new IllegalArgumentException("Niepoprawny status case: " + req.status() + ". Dozwolone: " + cfg.status().caseFlow());
        }

        // pola issue
        var fields = new HashMap<String, Object>();
        fields.put("summary", isNotBlank(req.summary()) ? req.summary() : req.case_id());
        fields.put("project", Map.of("key", jiraProps.getProjectKey()));
        fields.put("issuetype", Map.of("name", issueTypes.caseIssue()));
        fields.put("labels", req.labels() != null ? req.labels() : List.of());
        fields.put(fieldsCfg.caseId(), req.case_id());
        fields.put(fieldsCfg.payload(), req.payload().toString());

//        fields.put(fieldsCfg.templateId(), req.template_id());
//        fields.put("summary", ofNullable(req.summary()).orElse(""));
//        fields.put("description", ofNullable(req.description()).orElse(""));
//        fields.put(fieldsCfg.casePayload(), req.description());

        if (fieldsCfg.caseStatus() != null && !fieldsCfg.caseStatus().isBlank()) {
            fields.put(fieldsCfg.caseStatus(), req.status());
        }

        String jql = "%s ~ \"%s\""
                .formatted(
                        toJqlField(fieldsCfg.caseId()),
                        escapeJql(req.case_id())
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

        var url = jiraProps.getBaseUrl() + "/browse/" + issueKey;
        return new CaseRef(issueKey, url);
    }

    public void commentWithMentions(String caseKey, String text) {
        jira.addComment(caseKey, text);
    }

    public void vote(String caseKey, boolean up) {
        jira.setVote(caseKey, up);
    }

    public RatingResult rate(String caseKey, RatingInput rating) {
        var cfg = jiraAdminCfg.getForRuntime();
        var ratingField = cfg.fields().ratingAvg();

        double avg = rating.categories().stream().mapToDouble(RatingInput.Category::value).average().orElse(0.0);
        Map<String, Object> body = Map.of("fields", Map.of(ratingField, avg));
        jira.updateIssue(caseKey, body);
        return new RatingResult(avg);
    }

    // ───────────────── helpers ─────────────────

    private static final Pattern CUSTOMFIELD = Pattern.compile("^customfield_(\\d+)$");

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
