package com.acme.herald.config;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;

public final class JiraIntegrationConfigDtos {

    private JiraIntegrationConfigDtos() {}

    // ─────────────────────────────────────────────────────────────
    // PUBLIC DTO (OpenAPI)
    // ─────────────────────────────────────────────────────────────

    @Schema(description = "Optional global banner shown under the app header. When text is blank, banner is hidden.")
    public record UiBannerDto(
            @Schema(description = "Banner text (single line). When blank -> hidden.",
                    example = "🚧 Przerwa serwisowa 20:00–21:00 (wdrożenie).")
            String text,

            @Schema(description = "Background color (HEX), e.g. #ff897d. If invalid -> default is used.",
                    example = "#ff897d")
            String color
    ) {}

    @Schema(description = "Provider integration configuration used by the proxy.")
    public record JiraIntegrationConfigDto(

            @Schema(description = "Configuration schema version.", example = "1")
            Integer version,

            @Schema(description = "Provider base URL override. When empty, the proxy default is used.",
                    example = "https://jira.example.com")
            String baseUrl,

            @Schema(description = "Provider project key override. When empty, the proxy default is used.",
                    example = "ABC")
            String projectKey,

            @Schema(description = "Provider issue type names used by the proxy.",
                    example = "{\"epic\":\"Epic\",\"template\":\"Task\",\"caseIssue\":\"Story\",\"section\":\"Sub-task\"}")
            @Valid
            JiraIssueTypesConfigDto issueTypes,

            @Schema(description = "Provider field identifiers used by the proxy (e.g., built-in field names or custom field IDs).",
                    example = "{\"templateId\":\"customfield_10112\",\"caseId\":\"customfield_10113\",\"payload\":\"customfield_10114\",\"casePayload\":\"customfield_10301\",\"epicLink\":\"customfield_10101\",\"ratingAvg\":\"customfield_10115\",\"description\":\"description\",\"caseStatus\":\"customfield_12345\",\"templateStatus\":\"customfield_12346\"}")
            @Valid
            JiraFieldsConfigDto fields,

            @Schema(description = "Link type configuration used when connecting templates and cases.",
                    example = "{\"templateToCase\":\"Implements\"}")
            @Valid
            JiraLinksConfigDto links,

            @Schema(description = "Proxy behavior flags for Provider integration.",
                    example = "{\"proxyAttachmentContent\":true,\"attachHeaderNoCheck\":true,\"useOptimisticLock\":false}")
            @Valid
            JiraOptionsConfigDto options,

            @Schema(
                    description = """
                            Mapping of Herald status categories to Jira workflow status names.
                            
                            Herald uses Jira workflow statuses as the source of truth, but needs a stable abstraction:
                            - TODO: initial status (must exist)
                            - IN_PROGRESS: work in progress (must exist)
                            - DONE: completed (must exist)
                            - IN_REVIEW: optional, reserved for future approval workflows
                            - PUBLISHED: (templates only) makes template visible in Hub (must exist for templates)
                            - REJECTED: hidden state (issue remains in Jira but Herald should not show it)
                            - DEPRECATED: (templates only) informational state; cases based on this template should display a badge
                            
                            Keys are upper-case category identifiers. Values are exact Jira status names from the workflow,
                            e.g. "To Do", "In Progress", "Done", "Published".
                            """,
                    example = """
                            {
                              "caseStatusMap": {
                                "TODO": "To Do",
                                "IN_PROGRESS": "In Progress",
                                "DONE": "Done",
                                "REJECTED": "Rejected",
                                "IN_REVIEW": "In Review"
                              },
                              "templateStatusMap": {
                                "TODO": "To Do",
                                "IN_PROGRESS": "In Progress",
                                "DONE": "Done",
                                "PUBLISHED": "Published",
                                "REJECTED": "Rejected",
                                "DEPRECATED": "Deprecated",
                                "IN_REVIEW": "In Review"
                              }
                            }
                            """
            )
            @Valid
            JiraStatusConfigDto status,
            JiraAccessConfigDto access,
            @Schema(description = "Issue key used as a storage container for user profile preferences. When empty, profile updates are disabled.",
                    example = "ABC-1")
            String userPrefsIssueKey,
            @Schema(description = "Issue key used as a storage container for Template Hub Challenges (issue properties). When empty, challenges are disabled.",
                    example = "ABC-2")
            String challengesIssueKey,
            UiBannerDto banner
    ) {}

    // ─────────────────────────────────────────────────────────────
    // SUB-DTOS
    // ─────────────────────────────────────────────────────────────

    @Schema(description = "Provider issue type names used by the proxy.")
    public record JiraIssueTypesConfigDto(

            @Schema(description = "Issue type name used for epics.", example = "Epic")
            String epic,

            @Schema(description = "Issue type name used for templates.", example = "Task")
            String template,

            @Schema(description = "Issue type name used for cases.", example = "Story")
            String caseIssue,

            @Schema(description = "Issue type name used for sections.", example = "Sub-task")
            String section
    ) {}

    @Schema(description = "Provider field identifiers used by the proxy.")
    public record JiraFieldsConfigDto(

            @Schema(description = "Field identifier used to store templateId.", example = "customfield_10112")
            String templateId,

            @Schema(description = "Field identifier used to store caseId.", example = "customfield_10113")
            String caseId,

            @Schema(description = "Field identifier used to store raw JSON payload (stringified).", example = "customfield_10114")
            String payload,

            @Schema(description = "Field identifier used to store case payload in wiki/markup format.", example = "customfield_10301")
            String casePayload,

            @Schema(description = "Field identifier used to store epic link reference.", example = "customfield_10101")
            String epicLink,

            @Schema(description = "Field identifier used to store rating average (optional).", example = "customfield_10115")
            String ratingAvg,

            @Schema(description = "Field identifier used as a human-readable description field.", example = "description")
            String description,

            @Schema(description = "Custom field identifier used to store case workflow status (Herald mapping).",
                    example = "customfield_12345")
            String caseStatus,

            @Schema(description = "Custom field identifier used to store template workflow status (Herald mapping).",
                    example = "customfield_12346")
            String templateStatus
    ) {}

    @Schema(description = "Link type configuration used by the proxy.")
    public record JiraLinksConfigDto(

            @Schema(description = "Provider link type id used for linking templates to cases.")
            String templateToCase,
            @Schema(description = "Provider link type id used for linking templates to template fork")
            String templateToFork
    ) {}

    @Schema(description = "Integration options controlling proxy behavior.")
    public record JiraOptionsConfigDto(

            @Schema(description = "When true, attachment content is streamed via proxy endpoints instead of direct Provider URLs.",
                    example = "true")
            Boolean proxyAttachmentContent,

            @Schema(description = "When true, the proxy sends a no-check header for attachment upload when required by the Provider.",
                    example = "true")
            Boolean attachHeaderNoCheck,

            @Schema(description = "When true, the proxy attempts optimistic lock semantics where supported.",
                    example = "false")
            Boolean useOptimisticLock
    ) {}

    @Schema(description = "Status mapping configuration used by the proxy.")
    public record JiraStatusConfigDto(

            @Schema(
                    description = """
                            CASE status mapping: Herald category -> Provider workflow status name.
                            Required keys: TODO, IN_PROGRESS, DONE, REJECTED.
                            Optional key: IN_REVIEW.
                            """,
                    example = """
                            {
                              "TODO": "To Do",
                              "IN_PROGRESS": "In Progress",
                              "DONE": "Done",
                              "REJECTED": "Rejected",
                              "IN_REVIEW": "In Review"
                            }
                            """
            )
            Map<String, String> caseStatusMap,

            @Schema(
                    description = """
                            TEMPLATE status mapping: Herald category -> Provider workflow status name.
                            Required keys: TODO, IN_PROGRESS, DONE, PUBLISHED, REJECTED, DEPRECATED.
                            Optional key: IN_REVIEW.
                            Note: PUBLISHED controls visibility in Hub.
                            """,
                    example = """
                            {
                              "TODO": "To Do",
                              "IN_PROGRESS": "In Progress",
                              "DONE": "Done",
                              "PUBLISHED": "Published",
                              "REJECTED": "Rejected",
                              "DEPRECATED": "Deprecated",
                              "IN_REVIEW": "In Review"
                            }
                            """
            )
            Map<String, String> templateStatusMap
    ) {}

    public record JiraAccessConfigDto(
            List<String> allowGroups,
            List<String> denyGroups
    ) {}

    // ─────────────────────────────────────────────────────────────
    // INTERNAL STORAGE MODEL
    // (Project Property payload; can remain identical to public DTO structure)
    // ─────────────────────────────────────────────────────────────

    public record StoredJiraIntegration(
            Integer version,
            String baseUrl,
            String projectKey,
            JiraIssueTypesConfigDto issueTypes,
            JiraFieldsConfigDto fields,
            JiraLinksConfigDto links,
            JiraOptionsConfigDto options,
            JiraStatusConfigDto status,
            JiraAccessConfigDto access,
            String userPrefsIssueKey,
            String challengesIssueKey,
            UiBannerDto banner
    ) {}
}
