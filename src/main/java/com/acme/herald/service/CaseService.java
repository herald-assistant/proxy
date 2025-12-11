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
    private final JiraProperties cfg;

    public CaseRef createCase(CreateCase req) {
        var fields = Map.of(
                "project", Map.of("key", cfg.getProjectKey()),
                "summary", ofNullable(req.summary()).orElse(""),
                "description", ofNullable(req.description()).orElse(""),
                "issuetype", Map.of("name", cfg.getIssueTypes().get("case")),
                cfg.getFields().get("caseId"), req.case_id(),
                cfg.getFields().get("templateId"), req.template_id(),
                cfg.getFields().get("payload"), req.payload().toString(),
                cfg.getFields().get("casePayload"), req.casePayload(),
                "labels", req.labels() != null ? req.labels() : List.of()
        );

        var existing = jira.search("herald_case_id ~ " + req.case_id(), 0, 1);

        String issueKey;

        if (existing.total() == 0) {
            issueKey = jira.createIssue(Map.of("fields", fields)).key();
        } else {
            Map<String, Object> issue = existing.issues().stream().findAny().orElseThrow();
            issueKey = String.valueOf(issue.get("key"));
            jira.updateIssue(issueKey, Map.of("fields", fields), null);
        }

        var url = cfg.getBaseUrl() + "/browse/" + issueKey;
        return new CaseRef(issueKey, url);
    }

    public CaseRef updatePayload(String caseKey, int expectedVersion, String payloadJson) {
        // GET → sprawdź version, potem PUT (optimistic lock w providerze)
        // W Jirze custom field payload ustawiamy jako String (JSON)
        Map<String, Object> body = Map.of("fields", Map.of(cfg.getFields().get("payload"), payloadJson));
        jira.updateIssue(caseKey, body, expectedVersion);
        return new CaseRef(caseKey, cfg.getBaseUrl() + "/browse/" + caseKey);
    }

    public void transition(String caseKey, String transitionId) {
        jira.transition(caseKey, transitionId);
    }

    public JiraModels.TransitionList transitions(String caseKey) {
        return jira.transitions(caseKey);
    }

    public void commentWithMentions(String caseKey, String text) {
        // minimalnie: wrzucamy body tak jak jest; adapter Cloud może zamienić @user → accountId
        jira.addComment(caseKey, text);
    }

    public void vote(String caseKey, boolean up) {
        jira.setVote(caseKey, up);
    }

    public RatingResult rate(String caseKey, RatingInput rating) {
        // policz avg i zaktualizuj customfield ratingAvg
        double avg = rating.categories().stream().mapToDouble(RatingInput.Category::value).average().orElse(0.0);
        Map<String, Object> body = Map.of("fields", Map.of(cfg.getFields().get("ratingAvg"), avg));
        jira.updateIssue(caseKey, body, null);
        return new RatingResult(avg);
    }
}
