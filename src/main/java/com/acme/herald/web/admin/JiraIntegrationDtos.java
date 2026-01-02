package com.acme.herald.web.admin;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class JiraIntegrationDtos {

    public record JiraIntegrationDto(
            Integer version,
            String baseUrl,
            String projectKey,
            JiraIssueTypesDto issueTypes,
            JiraFieldsDto fields,
            JiraLinksDto links,
            JiraOptionsDto options,
            JiraStatusDto status,
            String userPrefsIssueKey
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
            String description,
            String caseStatus,
            String templateStatus
    ) {}

    public record JiraLinksDto(
            String templateToCase
    ) {}

    public record JiraOptionsDto(
            Boolean proxyAttachmentContent,
            Boolean attachHeaderNoCheck,
            Boolean useOptimisticLock
    ) {}

    public record StoredJiraIntegration(
            Integer version,
            String baseUrl,
            String projectKey,
            JiraIssueTypesDto issueTypes,
            JiraFieldsDto fields,
            JiraLinksDto links,
            JiraOptionsDto options,
            JiraStatusDto status,
            String userPrefsIssueKey
    ) {}

    public record JiraStatusDto(
            List<String> caseFlow,
            List<String> templateFlow
    ) {}
}
