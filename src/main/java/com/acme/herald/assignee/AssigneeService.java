package com.acme.herald.assignee;

import com.acme.herald.assignee.dto.AssigneeDtos;
import com.acme.herald.config.JiraProperties;
import com.acme.herald.domain.JiraModels;
import com.acme.herald.provider.JiraProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class AssigneeService {
    private final JiraProperties cfg;
    private final JiraProvider jira;

    public void assignIssue(String key, AssigneeDtos.AssigneeReq payload) {
        jira.assignIssue(key, payload);
    }

    public List<AssigneeDtos.AssignableUser> findAssignableUsers(String issueKey, String username, int startAt, int maxResults) {
        String projectKey = cfg.getProjectKey();
        var users = jira.findAssignableUsers(issueKey, projectKey, username, startAt, maxResults);
        return users.stream()
                .map(u -> new AssigneeDtos.AssignableUser(u.name(), u.key(), u.displayName(), u.emailAddress(), u.avatarUrls()))
                .toList();
    }
}

