package com.acme.herald.domain.dto;

import com.acme.herald.domain.JiraModels;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

import java.util.Map;

public class MeContextDtos {

    @Schema(description = "Current user context resolved via the Provider proxy.")
    public record MeContext(
            @Schema(description = "Current user information as returned by the Provider.", example = "{\"displayName\":\"Jane Doe\"}")
            JiraModels.UserResponse user,

            @Schema(description = "Configured project key used by the proxy.", example = "ABC")
            String projectKey,

            @Schema(description = "Resolved permission entries for the current user.", example = "{\"BROWSE_PROJECTS\":{\"havePermission\":true}}")
            Map<String, JiraModels.PermissionEntry> permissions,

            @Schema(description = "User profile preferences stored by the proxy.")
            UserProfilePrefs profilePrefs
    ) {}

    @Schema(description = "User profile preferences used by the proxy UI and automation features.")
    public record UserProfilePrefs(
            @Schema(description = "Free-form description of the user, used to tailor explanations and help content.",
                    example = "junior analyst, prefers short explanations")
            String explainUserDescription,

            @Schema(description = "Whether the user wants to receive notifications about newly published templates.",
                    example = "true")
            boolean notifyNewTemplates,

            @Schema(description = "Last update timestamp (ISO-8601). Null when never updated.",
                    example = "2026-01-04T16:12:33Z")
            String updatedAt,

            @Schema(description = "Whether GitHub Copilot token is stored for the user (token itself is never returned).",
                    example = "true")
            boolean githubCopilotTokenPresent
    ) {}

    @Schema(description = "Request payload for updating user profile preferences.")
    public record UpdateUserProfilePrefs(
            @Schema(description = "Free-form description of the user, used to tailor explanations and help content.",
                    example = "beginner business analyst")
            @Size(max = 1200)
            String explainUserDescription,

            @Schema(description = "Whether the user wants to receive notifications about newly published templates.",
                    example = "false")
            boolean notifyNewTemplates,

            @Schema(description = "Optional GitHub Copilot token (Fine-grained PAT with Copilot Requests). " +
                    "If null/blank -> token is not changed. Token is encrypted at rest and never returned.",
                    example = "github_pat_...")
            @Size(max = 2048)
            String githubCopilotToken,

            @Schema(description = "If true -> deletes stored GitHub Copilot token (wins over githubCopilotToken).",
                    example = "true")
            boolean clearGithubCopilotToken
    ) {}
}
