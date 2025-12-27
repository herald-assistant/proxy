package com.acme.herald.web.admin;

import com.fasterxml.jackson.annotation.JsonProperty;

public class JiraIntegrationDtos {

    public record JiraIntegrationDto(
            Integer version,
            JiraIssueTypesDto issueTypes,
            JiraFieldsDto fields,
            JiraLinksDto links,
            JiraOptionsDto options
    ) {}

    public record JiraIssueTypesDto(
            String epic,
            String template,
            @JsonProperty("case") String caseIssue, // JSON ma "case"
            String section
    ) {}

    public record JiraFieldsDto(
            String templateId,
            String caseId,
            String payload,
            String casePayload,
            String epicLink,
            String ratingAvg,
            String description
    ) {}

    public record JiraLinksDto(
            String templateToCase
    ) {}

    public record JiraOptionsDto(
            Boolean proxyAttachmentContent,
            Boolean attachHeaderNoCheck,
            Boolean useOptimisticLock
    ) {}

    // To trzymamy w Jira Project Property (identyczny shape, łatwo migrować)
    public record StoredJiraIntegration(
            Integer version,
            JiraIssueTypesDto issueTypes,
            JiraFieldsDto fields,
            JiraLinksDto links,
            JiraOptionsDto options
    ) {}
}
