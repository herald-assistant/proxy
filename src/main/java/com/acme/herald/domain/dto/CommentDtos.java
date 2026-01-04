package com.acme.herald.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public final class CommentDtos {

    private CommentDtos() {}

    // ───── API MODELS (visible to clients) ─────

    @Schema(description = "Anchor information pointing to a fragment of content the comment refers to.")
    public record CommentAnchor(
            @Schema(description = "Anchor type.", example = "textRange")
            String type,

            @Schema(description = "Start position in the referenced content (inclusive).", example = "120")
            Integer from,

            @Schema(description = "End position in the referenced content (exclusive).", example = "180")
            Integer to,

            @Schema(description = "Short excerpt of the referenced content for quick context.", example = "This fragment describes the main decision rule...")
            String snippet
    ) {}

    @Schema(description = "Single comment within a thread.")
    public record Comment(
            @Schema(description = "Herald comment identifier.", example = "c-3f6d5a1e")
            String id,

            @Schema(description = "Author display name.", example = "Jane Doe")
            String author,

            @Schema(description = "Rendered comment text to display (Provider markup).", example = "h3. Review\\nLooks good, but please add examples.")
            String text,

            @Schema(description = "Optional editor body (TipTap JSON).", example = "{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"Looks good\"}]}]}")
            Object body,

            @Schema(description = "Creation timestamp (ISO-8601).", example = "2026-01-04T16:12:33Z")
            String createdAt,

            @Schema(description = "Last update timestamp (ISO-8601).", example = "2026-01-04T16:20:10Z")
            String updatedAt
    ) {}

    @Schema(description = "Comment thread attached to a specific anchor within an issue/case.")
    public record Thread(
            @Schema(description = "Thread identifier.", example = "t-7c7c2b64")
            String id,

            @Schema(description = "Case identifier (usually equals issueKey unless explicitly provided).", example = "ABC-123")
            String caseId,

            @Schema(description = "Anchor describing what the thread refers to.")
            CommentAnchor anchor,

            @Schema(description = "Thread creator display name.", example = "Jane Doe")
            String createdBy,

            @Schema(description = "Thread creation timestamp (ISO-8601).", example = "2026-01-04T16:12:33Z")
            String createdAt,

            @Schema(description = "Whether the thread is resolved.", example = "false")
            boolean resolved,

            @Schema(description = "Thread comments in chronological order.")
            List<Comment> comments
    ) {}

    @Schema(description = "Fetch response containing all threads for a given issue.")
    public record FetchRes(
            @Schema(description = "List of comment threads.", example = "[{\"id\":\"t-1\",\"caseId\":\"ABC-123\",\"resolved\":false,\"comments\":[]}]")
            List<Thread> threads
    ) {}

    // ───── REQUEST MODELS ─────

    @Schema(description = "Request payload for adding a new root thread comment.")
    public record AddRootCommentReq(
            @Schema(description = "Optional anchor identifying the target fragment the thread refers to.")
            CommentAnchor anchor,

            @Schema(description = "Plain text fallback for rendering.", example = "Please clarify this requirement.")
            String text,

            @Schema(description = "Optional editor body (TipTap JSON).", example = "{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\"}]}")
            Object body
    ) {}

    @Schema(description = "Request payload for replying to an existing thread.")
    public record ReplyReq(
            @Schema(description = "Plain text fallback for rendering.", example = "Acknowledged, will update.")
            String text,

            @Schema(description = "Optional editor body (TipTap JSON).", example = "{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\"}]}")
            Object body
    ) {}

    @Schema(description = "Request payload for editing an existing comment.")
    public record EditReq(
            @Schema(description = "Plain text fallback for rendering.", example = "Updated with additional details.")
            String text,

            @Schema(description = "Optional editor body (TipTap JSON).", example = "{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\"}]}")
            Object body
    ) {}

    @Schema(description = "Request payload for resolving or reopening a thread.")
    public record ResolveReq(
            @Schema(description = "true = resolved, false = unresolved.", example = "true")
            boolean resolved
    ) {}

    // ───── INTERNAL META STORED IN PROVIDER ISSUE PROPERTY ─────

    @Schema(description = "Internal comment metadata stored in a Provider issue property.")
    public record CommentMeta(
            @Schema(description = "Herald comment identifier.", example = "c-3f6d5a1e")
            String id,

            @Schema(description = "Provider comment identifier.", example = "10100")
            String jiraCommentId,

            @Schema(description = "Author display name.", example = "Jane Doe")
            String author,

            @Schema(description = "Optional editor body (TipTap JSON).", example = "{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\"}]}")
            Object body,

            @Schema(description = "Creation timestamp (ISO-8601).", example = "2026-01-04T16:12:33Z")
            String createdAt,

            @Schema(description = "Last update timestamp (ISO-8601).", example = "2026-01-04T16:20:10Z")
            String updatedAt
    ) {}

    @Schema(description = "Internal thread metadata stored in a Provider issue property.")
    public record ThreadMeta(
            @Schema(description = "Thread identifier.", example = "t-7c7c2b64")
            String id,

            @Schema(description = "Case identifier.", example = "ABC-123")
            String caseId,

            @Schema(description = "Anchor describing what the thread refers to.")
            CommentAnchor anchor,

            @Schema(description = "Thread creator display name.", example = "Jane Doe")
            String createdBy,

            @Schema(description = "Thread creation timestamp (ISO-8601).", example = "2026-01-04T16:12:33Z")
            String createdAt,

            @Schema(description = "Whether the thread is resolved.", example = "false")
            boolean resolved,

            @Schema(description = "List of internal comment metadata.")
            List<CommentMeta> comments
    ) {}

    @Schema(description = "Internal property value stored under a fixed Provider issue property key.")
    public record PropertyValue(
            @Schema(description = "List of internal thread metadata.")
            List<ThreadMeta> threads
    ) {}
}
