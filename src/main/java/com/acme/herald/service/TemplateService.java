package com.acme.herald.service;

import com.acme.herald.config.JiraProperties;
import com.acme.herald.domain.JiraModels;
import com.acme.herald.domain.dto.CreateTemplate;
import com.acme.herald.domain.dto.TemplateRef;
import com.acme.herald.provider.JiraProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class TemplateService {
    private final JiraProvider jira;
    private final JiraProperties cfg;

    public TemplateRef createTemplate(CreateTemplate req) {
        var fields = Map.of(
                "project", Map.of("key", cfg.getProjectKey()),
                "summary", req.title(),
                "issuetype", Map.of("name", cfg.getIssueTypes().get("template")),
                cfg.getFields().get("templateId"), req.template_id(),
                "labels", req.labels() != null ? req.labels() : java.util.List.of(),
                cfg.getFields().get("payload"), req.payload().toString()
        );
        var existing = jira.search("herald_template_id ~ " + req.template_id() + " AND herald_case_id is EMPTY", 0, 1);

        String issueKey;
        if (existing.total() == 0) {
            issueKey = jira.createIssue(Map.of("fields", fields)).key();
        } else {
            Map<String, Object> issue = existing.issues().stream().findAny().orElseThrow();
            issueKey = String.valueOf(issue.get("key"));
            jira.updateIssue(issueKey, Map.of("fields", fields), null);
        }
        String url = cfg.getBaseUrl() + "/browse/" + issueKey;
        return new TemplateRef(issueKey, url);
    }

    public void transition(String caseKey, String transitionId) {
        jira.transition(caseKey, transitionId);
    }

    public JiraModels.TransitionList transitions(String caseKey) {
        return jira.transitions(caseKey);
    }
}
