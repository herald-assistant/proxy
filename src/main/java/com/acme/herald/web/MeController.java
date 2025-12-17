package com.acme.herald.web;

import com.acme.herald.config.JiraProperties;
import com.acme.herald.domain.JiraModels;
import com.acme.herald.provider.JiraProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/me", produces = MediaType.APPLICATION_JSON_VALUE)
public class MeController {
    private final JiraProvider jira;
    private final JiraProperties jiraProps;

    @GetMapping("/context")
    public MeContext context() {
        var user = jira.getMe();
        var perms = jira.getMyPermissions(jiraProps.getProjectKey(), null, null);
        return new MeContext(user, jiraProps.getProjectKey(), perms.permissions());
    }

    public record MeContext(
            JiraModels.UserResponse user,
            String projectKey,
            java.util.Map<String, JiraModels.PermissionEntry> permissions
    ) {}
}