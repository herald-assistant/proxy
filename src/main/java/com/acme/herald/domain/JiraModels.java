package com.acme.herald.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

public class JiraModels {
    public record JiraPatCreateRequest(String name, int expirationDuration) {
    }

    public record JiraPatCreateResponse(Long id, String name, String rawToken, String expiringAt) {
    }

    public record UserResponse(
            String key,
            String name,
            String emailAddress,
            String displayName,
            Map<String, String> avatarUrls,

            Boolean active,
            String timeZone,
            String locale,

            SimpleListWrapper groups
    ) {
    }

    public record SimpleListWrapper(
            Integer size,
            @JsonProperty("max-results") Integer maxResults,
            List<GroupItem> items
    ) {
    }

    public record GroupItem(
            String name,
            String self
    ) {
    }

    public record PermissionsResponse(
            Map<String, PermissionEntry> permissions
    ) {
    }

    public record PermissionEntry(
            Boolean havePermission,
            String id,
            String key,
            String name,
            String type
    ) {
    }

    public record Issue(String id, String key, Map<String, Object> fields) {
    }

    public record IssueRef(String id, String key, Integer version) {
    }

    public record CreateIssueResponse(String id, String key, String self) {
    }

    public record SearchResponse(int startAt, int maxResults, int total, List<JsonNode> issues) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ChangelogPageCore(
            int startAt,
            int maxResults,
            int total,
            List<JiraModels.ChangelogHistory> histories
    ) { }

    public record ChangelogPage(
            int startAt,
            int maxResults,
            int total,
            Map<String, String> fieldNames,
            List<ChangelogHistory> histories
    ) { }

    public record ChangelogHistory(
            String id,
            String created,
            ChangelogAuthor author,
            List<ChangelogHistoryItem> items
    ) { }

    public record ChangelogAuthor(
            String key,
            String name,
            String accountId,
            String displayName
    ) { }

    public record ChangelogHistoryItem(
            @JsonProperty("field") String fieldName,          // nazwa pola (label)
            @JsonProperty("fieldtype") String fieldType,      // typ
            String from,
            @JsonProperty("fromString") String fromText,      // NIE fromString()
            String to,
            @JsonProperty("toString") String toText
    ) { }

    public record AssigneePayload(String name, String key) {
    }


    public record AssignableUser(
            String name,
            String key,
            String displayName,
            String emailAddress,
            Map<String, String> avatarUrls
    ) {
    }

    public record Attachment(
            String id,
            String self,
            String filename,
            long size,
            String mimeType,
            String content,      // pełny URL do pobrania treści
            String thumbnail     // pełny URL miniatury (jeśli jest)
    ) {
    }

    public record AttachmentList(List<Attachment> value) {
    }

    public record CommentAuthor(
            String key,
            String name,
            String displayName
    ) {
    }

    public record Comment(
            String id,
            CommentAuthor author,
            String body,
            String created,
            String updated
    ) {
    }

    public record CommentPage(
            int startAt,
            int maxResults,
            int total,
            List<Comment> comments
    ) {
    }

    public record IssueLinkTypes(
            List<IssueLinkType> issueLinkTypes
    ) {}

    public record IssueLinkType(
            String id,
            String name,
            String inward,
            String outward
    ) {}

}