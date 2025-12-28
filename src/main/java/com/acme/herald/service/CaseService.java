package com.acme.herald.service;

import com.acme.herald.config.JiraProperties;
import com.acme.herald.domain.JiraModels;
import com.acme.herald.domain.dto.CaseRef;
import com.acme.herald.domain.dto.CreateCase;
import com.acme.herald.domain.dto.RatingInput;
import com.acme.herald.domain.dto.RatingResult;
import com.acme.herald.provider.JiraProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import static java.util.Optional.ofNullable;

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

        var fields = Map.of(
                "project", Map.of("key", jiraProps.getProjectKey()),
                "summary", ofNullable(req.summary()).orElse(""),
                "description", ofNullable(req.description()).orElse(""),
                "issuetype", Map.of("name", issueTypes.caseIssue()),
                fieldsCfg.caseId(), req.case_id(),
                fieldsCfg.templateId(), req.template_id(),
                fieldsCfg.payload(), req.payload().toString(),
                fieldsCfg.casePayload(), req.casePayload(),
                "labels", req.labels() != null ? req.labels() : List.of()
        );

        var existing = jira.search("herald_case_id ~ " + req.case_id(), 0, 1);

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
}
