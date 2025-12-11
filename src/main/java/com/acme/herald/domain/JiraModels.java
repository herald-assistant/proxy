package com.acme.herald.domain;

import java.util.List;
import java.util.Map;

public class JiraModels {
    public record UserResponse(String key, String name, String emailAddress, String displayName,
                               Map<String, String> avatarUrls) {
    }

    public record Issue(String id, String key, Map<String, Object> fields) {
    }

    public record IssueRef(String id, String key, Integer version) {
    }

    public record CreateIssueResponse(String id, String key, String self) {
    }

    public record SearchResponse(int startAt, int maxResults, int total, List<Map<String, Object>> issues) {
    }

    public record ChangelogItem(String field, String from, String to) {
    }

    public record TransitionList(List<Transition> transitions) {
    }

    public record Transition(String id,
                             String name,
                             Status to,
                             Boolean isGlobal,
                             Boolean isInitial,
                             Boolean hasScreen,
                             Map<String, Object> fields) {
    }


    public record Status(String id,
                         String name,
                         StatusCategory statusCategory) {
    }

    public record StatusCategory(Integer id,
                                 String key,
                                 String colorName,
                                 String name) {
    }

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

    public record AssignableUserList(List<AssignableUser> users) {
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

}