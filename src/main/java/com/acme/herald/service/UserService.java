package com.acme.herald.service;

import com.acme.herald.config.JiraProperties;
import com.acme.herald.domain.JiraModels;
import com.acme.herald.provider.JiraProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@RequiredArgsConstructor
public class UserService {
    private final JiraProperties cfg;
    private final JiraProvider jira;

    public void assignIssue(String key, JiraModels.AssigneePayload payload) {
        jira.assignIssue(key, payload);
    }

    public List<JiraModels.AssignableUser> findAssignableUsers(String issueKey, String username, int startAt, int maxResults) {
        String projectKey = cfg.getProjectKey();
        return jira.findAssignableUsers(issueKey, projectKey, username, startAt, maxResults);
    }
}

