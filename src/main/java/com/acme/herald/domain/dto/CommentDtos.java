// package com.acme.herald.domain.dto;
package com.acme.herald.domain.dto;

import java.util.List;

public final class CommentDtos {

    // ───── MODELE API (to widzi front) ─────

    public record CommentAnchor(
            String type,     // "global" | "selection" | "block"
            Integer from,
            Integer to,
            String snippet
    ) {}

    public record Comment(
            String id,          // Herald commentId (UUID)
            String author,
            String text,
            String createdAt,
            String updatedAt
    ) {}

    public record Thread(
            String id,                 // threadId (UUID)
            String caseId,             // issueKey
            CommentAnchor anchor,
            String createdBy,
            String createdAt,
            boolean resolved,
            List<Comment> comments
    ) {}

    public record FetchRes(
            List<Thread> threads
    ) {}

    // ───── REQUESTY API ─────

    public record AddRootCommentReq(
            CommentAnchor anchor,
            String text
    ) {}

    public record ReplyReq(
            String text
    ) {}

    public record EditReq(
            String text
    ) {}

    public record ResolveReq(
            boolean resolved
    ) {}

    // ───── META do issue property (bez tekstu!) ─────

    public record CommentMeta(
            String id,            // Herald commentId (UUID)
            String jiraCommentId, // ID komentarza w Jirze
            String author,
            String createdAt,
            String updatedAt
    ) {}

    public record ThreadMeta(
            String id,
            String caseId,
            CommentAnchor anchor,
            String createdBy,
            String createdAt,
            boolean resolved,
            List<CommentMeta> comments
    ) {}

    public record PropertyValue(
            List<ThreadMeta> threads
    ) {}
}
