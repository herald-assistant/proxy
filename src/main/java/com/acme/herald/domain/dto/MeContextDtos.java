package com.acme.herald.domain.dto;

import com.acme.herald.domain.JiraModels;
import jakarta.validation.constraints.Size;

public class MeContextDtos {
    public record MeContext(
            JiraModels.UserResponse user,
            String projectKey,
            java.util.Map<String, JiraModels.PermissionEntry> permissions,
            UserProfilePrefs profilePrefs
    ) {}

    public record UserProfilePrefs(
            String explainUserDescription,
            boolean notifyNewTemplates,
            String updatedAt
    ) {}

    public record UpdateUserProfilePrefs(
            @Size(max = 1200) String explainUserDescription,
            boolean notifyNewTemplates
    ) {}
}
