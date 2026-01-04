package com.acme.herald.config;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;

import java.util.List;

public final class JiraIntegrationConfigDtos {

    private JiraIntegrationConfigDtos() {}

    @Schema(description = "Provider integration configuration used by the proxy.")
    public record JiraIntegrationConfigDto(

            @Schema(description = "Configuration schema version.", example = "1")
            Integer version,

            @Schema(description = "Provider base URL override. When empty, the proxy default is used.", example = "https://provider.example.com")
            String baseUrl,

            @Schema(description = "Provider project key override. When empty, the proxy default is used.", example = "ABC")
            String projectKey,

            @Schema(description = "Provider issue type names used by the proxy.", example = "{\"epic\":\"Epic\",\"template\":\"Task\",\"caseIssue\":\"Story\",\"section\":\"Sub-task\"}")
            @Valid
            JiraIssueTypesConfigDto issueTypes,

            @Schema(description = "Provider field identifiers used by the proxy (e.g., built-in field names or custom field IDs).",
                    example = "{\"templateId\":\"summary\",\"caseId\":\"summary\",\"payload\":\"description\",\"casePayload\":\"customfield_12345\",\"epicLink\":\"customfield_10008\",\"ratingAvg\":\"customfield_20001\",\"description\":\"description\",\"caseStatus\":\"customfield_30001\",\"templateStatus\":\"customfield_30002\"}")
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

            @Schema(description = "Allowed status values used by template/case flows.",
                    example = "{\"caseFlow\":[\"todo\",\"in_progress\",\"done\"],\"templateFlow\":[\"todo\",\"in_progress\",\"published\"]}")
            @Valid
            JiraStatusConfigDto status,

            @Schema(description = "Issue key used as a storage container for user profile preferences. When empty, profile updates are disabled.",
                    example = "ABC-1")
            String userPrefsIssueKey
    ) {}

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

            @Schema(description = "Field identifier used to store templateId.", example = "customfield_10010")
            String templateId,

            @Schema(description = "Field identifier used to store caseId.", example = "customfield_10011")
            String caseId,

            @Schema(description = "Field identifier used to store raw JSON payload (stringified).", example = "customfield_10012")
            String payload,

            @Schema(description = "Field identifier used to store case payload in wiki/markup format.", example = "customfield_10013")
            String casePayload,

            @Schema(description = "Field identifier used to store epic link reference.", example = "customfield_10008")
            String epicLink,

            @Schema(description = "Field identifier used to store rating average (optional).", example = "customfield_20001")
            String ratingAvg,

            @Schema(description = "Field identifier used as a human-readable description field.", example = "description")
            String description,

            @Schema(description = "Field identifier used to store case status.", example = "customfield_30001")
            String caseStatus,

            @Schema(description = "Field identifier used to store template status.", example = "customfield_30002")
            String templateStatus
    ) {}

    @Schema(description = "Link type configuration used by the proxy.")
    public record JiraLinksConfigDto(

            @Schema(description = "Provider link type name used for linking templates to cases.", example = "Implements")
            String templateToCase
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

    @Schema(description = "Allowed status flows used by the proxy.")
    public record JiraStatusConfigDto(

            @Schema(description = "Allowed statuses for case lifecycle.", example = "[\"todo\",\"in_progress\",\"done\",\"rejected\"]")
            List<String> caseFlow,

            @Schema(description = "Allowed statuses for template lifecycle.", example = "[\"todo\",\"in_progress\",\"published\",\"rejected\"]")
            List<String> templateFlow
    ) {}

    // ─────────────────────────────────────────────────────────────
    // INTERNAL STORAGE MODEL (optional)
    //
    // Jeśli trzymasz to jako project property i też chcesz mieć to w jednym pliku,
    // możesz zostawić tu "StoredJiraIntegration" jako record bez @Schema.
    // OpenAPI i tak będzie bazował na JiraIntegrationConfigDto.
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
            String userPrefsIssueKey
    ) {}
}
