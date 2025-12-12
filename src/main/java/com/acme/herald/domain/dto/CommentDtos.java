// package com.acme.herald.domain.dto;
package com.acme.herald.domain.dto;

import java.util.List;

public final class CommentDtos {

    // ───── MODELE API (to widzi front) ─────

    public record CommentAnchor(
            String type,
            Integer from,
            Integer to,
            String snippet
    ) {}

    public record Comment(
            String id,          // Herald commentId
            String author,
            String text,        // jira body (wiki) do wyświetlenia
            Object body,        // TipTap doc JSON do edycji (może być null)
            String createdAt,
            String updatedAt
    ) {}

    public record Thread(
            String id,
            String caseId,
            CommentAnchor anchor,
            String createdBy,
            String createdAt,
            boolean resolved,
            List<Comment> comments
    ) {}

    public record FetchRes(List<Thread> threads) {}

    // ───── REQUESTY API ─────

    public record AddRootCommentReq(
            CommentAnchor anchor,
            String text,   // plain fallback
            Object body    // NEW: TipTap JSON doc (Map)
    ) {}

    public record ReplyReq(
            String text,
            Object body    // NEW
    ) {}

    public record EditReq(
            String text,
            Object body    // NEW
    ) {}

    public record ResolveReq(boolean resolved) {}

    // ───── META do issue property ─────

    public record CommentMeta(
            String id,
            String jiraCommentId,
            String author,
            Object body,        // TipTap doc json
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

    public record PropertyValue(List<ThreadMeta> threads) {}
}
