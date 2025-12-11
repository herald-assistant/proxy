package com.acme.herald.service;

import com.acme.herald.config.JiraProperties;
import com.acme.herald.domain.dto.CreateSection;
import com.acme.herald.domain.dto.SectionRef;
import com.acme.herald.provider.JiraProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service @RequiredArgsConstructor
public class SectionService {
    private final JiraProvider jira;
    private final JiraProperties cfg;

    public SectionRef createSection(String caseKey, CreateSection req) {
        var fields = Map.of(
                "project", Map.of("key", cfg.getProjectKey()),
                "summary", req.title(),
                "issuetype", Map.of("name", cfg.getIssueTypes().get("section")),
                "parent", Map.of("key", caseKey)
        );
        var ref = jira.createIssue(Map.of("fields", fields));
        return new SectionRef(ref.key(), cfg.getBaseUrl()+"/browse/"+ref.key());
    }
}
