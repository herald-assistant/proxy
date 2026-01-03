package com.acme.herald.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class JiraIntegrationConfigDtos {

    public record JiraIntegrationConfigDto(
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

    public record JiraIssueTypesConfigDto(
            String epic,
            String template,
            @JsonProperty("case") String caseIssue, // JSON ma "case"
            String section
    ) {}

    public record JiraFieldsConfigDto(
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

    public record JiraLinksConfigDto(
            String templateToCase
    ) {}

    public record JiraOptionsConfigDto(
            Boolean proxyAttachmentContent,
            Boolean attachHeaderNoCheck,
            Boolean useOptimisticLock
    ) {}

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

    public record JiraStatusConfigDto(
            List<String> caseFlow,
            List<String> templateFlow
    ) {}
}
