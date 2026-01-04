// package com.acme.herald.service;
package com.acme.herald.comment;

import com.acme.herald.domain.JiraModels;
import com.acme.herald.domain.dto.CommentDtos;
import com.acme.herald.provider.JiraProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {

    private static final String PROPERTY_KEY = "herald.comments.v1";

    private final JiraProvider jira;
    private final JsonMapper jsonMapper;

    // ────────── PUBLIC API (używane przez CommentController) ──────────

    public CommentDtos.FetchRes fetch(String issueKey) {
        var meta = readProperty(issueKey);
        var jiraComments = jira.getComments(issueKey);

        return toFetchRes(issueKey, meta, jiraComments);
    }

    public CommentDtos.FetchRes addRootComment(String issueKey, CommentDtos.AddRootCommentReq body) {
        var meta = readProperty(issueKey);
        var me = jira.getMe();
        var author = authorName(me);
        var now = Instant.now().toString();

        var threads = new ArrayList<>(
                meta.threads() != null ? meta.threads() : List.<CommentDtos.ThreadMeta>of()
        );

        String threadId = UUID.randomUUID().toString();
        String commentId = UUID.randomUUID().toString();

        // 1) tworzymy Jira comment
        String jiraBody = renderBody(body.anchor(), body.text(), body.body(), false);
        JiraModels.Comment jiraComment = jira.addComment(issueKey, jiraBody);

        // 2) meta do property
        CommentDtos.CommentMeta commentMeta = new CommentDtos.CommentMeta(
                commentId,
                jiraComment.id(),
                author,
                body.body(), // NEW
                jiraComment.created() != null ? jiraComment.created() : now,
                jiraComment.updated() != null ? jiraComment.updated() : now
        );

        CommentDtos.ThreadMeta threadMeta = new CommentDtos.ThreadMeta(
                threadId,
                issueKey,
                body.anchor(),
                author,
                now,
                false,
                List.of(commentMeta)
        );

        threads.add(threadMeta);
        var nextMeta = new CommentDtos.PropertyValue(threads);
        writeProperty(issueKey, nextMeta);

        // 3) zbuduj FetchRes na podstawie meta + komentarzy z Jiry
        var jiraComments = jira.getComments(issueKey); // ma już nowy komentarz
        return toFetchRes(issueKey, nextMeta, jiraComments);
    }

    public CommentDtos.FetchRes reply(String issueKey, String threadId, CommentDtos.ReplyReq body) {
        var meta = readProperty(issueKey);
        var me = jira.getMe();
        var author = authorName(me);
        var now = Instant.now().toString();

        var threads = new ArrayList<>(
                meta.threads() != null ? meta.threads() : List.<CommentDtos.ThreadMeta>of()
        );

        var updatedThreads = new ArrayList<CommentDtos.ThreadMeta>();
        CommentDtos.ThreadMeta targetThread;

        for (CommentDtos.ThreadMeta t : threads) {
            if (!Objects.equals(t.id(), threadId)) {
                updatedThreads.add(t);
                continue;
            }

            // 1) Jira comment
            String jiraBody = renderBody(t.anchor(), body.text(), body.body(), true);
            JiraModels.Comment jiraComment = jira.addComment(issueKey, jiraBody);

            String commentId = UUID.randomUUID().toString();
            CommentDtos.CommentMeta cm = new CommentDtos.CommentMeta(
                    commentId,
                    jiraComment.id(),
                    author,
                    body.body(),
                    jiraComment.created() != null ? jiraComment.created() : now,
                    jiraComment.updated() != null ? jiraComment.updated() : now
            );

            var newComments = new ArrayList<>(
                    t.comments() != null ? t.comments() : List.<CommentDtos.CommentMeta>of()
            );
            newComments.add(cm);

            targetThread = new CommentDtos.ThreadMeta(
                    t.id(),
                    t.caseId(),
                    t.anchor(),
                    t.createdBy(),
                    t.createdAt(),
                    t.resolved(),
                    newComments
            );
            updatedThreads.add(targetThread);
        }

        var nextMeta = new CommentDtos.PropertyValue(updatedThreads);
        writeProperty(issueKey, nextMeta);

        var jiraComments = jira.getComments(issueKey);
        return toFetchRes(issueKey, nextMeta, jiraComments);
    }

    public CommentDtos.FetchRes edit(String issueKey, String threadId, String commentId, CommentDtos.EditReq body) {
        var meta = readProperty(issueKey);
        var threads = meta.threads() != null ? meta.threads() : List.<CommentDtos.ThreadMeta>of();

        var updatedThreads = new ArrayList<CommentDtos.ThreadMeta>();
        String jiraCommentId = null;

        for (CommentDtos.ThreadMeta t : threads) {
            if (!Objects.equals(t.id(), threadId)) {
                updatedThreads.add(t);
                continue;
            }

            var newComments = new ArrayList<CommentDtos.CommentMeta>();
            for (CommentDtos.CommentMeta c : t.comments()) {
                if (!Objects.equals(c.id(), commentId)) {
                    newComments.add(c);
                } else {
                    jiraCommentId = c.jiraCommentId();
                    // updatedAt uzupełnimy po odpowiedzi z Jiry
                    newComments.add(c);
                }
            }

            updatedThreads.add(new CommentDtos.ThreadMeta(
                    t.id(),
                    t.caseId(),
                    t.anchor(),
                    t.createdBy(),
                    t.createdAt(),
                    t.resolved(),
                    newComments
            ));
        }

        if (jiraCommentId != null) {
            // 1) update Jira comment
            // Szukamy anchor wątku, żeby zachować ewentualny kontekst
            CommentDtos.ThreadMeta t = updatedThreads.stream()
                    .filter(tt -> Objects.equals(tt.id(), threadId))
                    .findFirst()
                    .orElse(null);
            var anchor = t != null ? t.anchor() : null;

            String jiraBody = renderBody(anchor, body.text(), body.body(), false);
            JiraModels.Comment updated = jira.updateComment(issueKey, jiraCommentId, jiraBody);

            // 2) update updatedAt w meta
            var threadsWithUpdatedMeta = new ArrayList<CommentDtos.ThreadMeta>();
            for (CommentDtos.ThreadMeta tt : updatedThreads) {
                if (!Objects.equals(tt.id(), threadId)) {
                    threadsWithUpdatedMeta.add(tt);
                    continue;
                }
                var cmUpdated = new ArrayList<CommentDtos.CommentMeta>();
                for (CommentDtos.CommentMeta c : tt.comments()) {
                    if (!Objects.equals(c.id(), commentId)) {
                        cmUpdated.add(c);
                    } else {
                        cmUpdated.add(new CommentDtos.CommentMeta(
                                c.id(),
                                c.jiraCommentId(),
                                c.author(),
                                body.body(), // NEW
                                c.createdAt(),
                                updated.updated() != null ? updated.updated() : c.updatedAt()
                        ));
                    }
                }
                threadsWithUpdatedMeta.add(new CommentDtos.ThreadMeta(
                        tt.id(),
                        tt.caseId(),
                        tt.anchor(),
                        tt.createdBy(),
                        tt.createdAt(),
                        tt.resolved(),
                        cmUpdated
                ));
            }

            meta = new CommentDtos.PropertyValue(threadsWithUpdatedMeta);
            writeProperty(issueKey, meta);
        }

        var jiraComments = jira.getComments(issueKey);
        return toFetchRes(issueKey, meta, jiraComments);
    }

    public CommentDtos.FetchRes delete(String issueKey, String threadId, String commentId) {
        var meta = readProperty(issueKey);
        var threads = meta.threads() != null ? meta.threads() : List.<CommentDtos.ThreadMeta>of();

        String jiraCommentId = null;
        var updatedThreads = new ArrayList<CommentDtos.ThreadMeta>();

        for (CommentDtos.ThreadMeta t : threads) {
            if (!Objects.equals(t.id(), threadId)) {
                updatedThreads.add(t);
                continue;
            }

            var remaining = new ArrayList<CommentDtos.CommentMeta>();
            for (CommentDtos.CommentMeta c : t.comments()) {
                if (Objects.equals(c.id(), commentId)) {
                    jiraCommentId = c.jiraCommentId();
                } else {
                    remaining.add(c);
                }
            }

            if (!remaining.isEmpty()) {
                updatedThreads.add(new CommentDtos.ThreadMeta(
                        t.id(),
                        t.caseId(),
                        t.anchor(),
                        t.createdBy(),
                        t.createdAt(),
                        t.resolved(),
                        remaining
                ));
            }
            // jeśli nie ma komentarzy -> wątek znika
        }

        if (jiraCommentId != null) {
            jira.deleteComment(issueKey, jiraCommentId);
        }

        var nextMeta = new CommentDtos.PropertyValue(updatedThreads);
        writeProperty(issueKey, nextMeta);

        var jiraComments = jira.getComments(issueKey);
        return toFetchRes(issueKey, nextMeta, jiraComments);
    }

    public CommentDtos.FetchRes resolve(String issueKey, String threadId, CommentDtos.ResolveReq body) {
        var meta = readProperty(issueKey);
        var threads = meta.threads() != null ? meta.threads() : List.<CommentDtos.ThreadMeta>of();

        var updatedThreads = new ArrayList<CommentDtos.ThreadMeta>();
        for (CommentDtos.ThreadMeta t : threads) {
            if (!Objects.equals(t.id(), threadId)) {
                updatedThreads.add(t);
                continue;
            }
            updatedThreads.add(new CommentDtos.ThreadMeta(
                    t.id(),
                    t.caseId(),
                    t.anchor(),
                    t.createdBy(),
                    t.createdAt(),
                    body.resolved(),
                    t.comments()
            ));
        }

        var nextMeta = new CommentDtos.PropertyValue(updatedThreads);
        writeProperty(issueKey, nextMeta);

        var jiraComments = jira.getComments(issueKey);
        return toFetchRes(issueKey, nextMeta, jiraComments);
    }

    // ────────── HELPERY: meta <-> Jira -> FetchRes ──────────

    private CommentDtos.PropertyValue readProperty(String issueKey) {
        try {
            JsonNode val = jira.getIssueProperty(issueKey, PROPERTY_KEY); // <-- już value
            if (val != null && !val.isNull() && !val.isMissingNode() && !(val.isObject() && val.size() == 0)) {
                return jsonMapper.treeToValue(val, CommentDtos.PropertyValue.class);
            }
        } catch (Exception ignored) {}
        return new CommentDtos.PropertyValue(List.of());
    }

    private void writeProperty(String issueKey, CommentDtos.PropertyValue value) {
        jira.setIssueProperty(issueKey, PROPERTY_KEY, value);
    }

    private CommentDtos.FetchRes toFetchRes(
            String issueKey,
            CommentDtos.PropertyValue meta,
            List<JiraModels.Comment> jiraComments
    ) {
        Map<String, JiraModels.Comment> byId = jiraComments.stream()
                .collect(Collectors.toMap(JiraModels.Comment::id, Function.identity(), (a, b) -> a));

        var threadsMeta = meta.threads() != null ? meta.threads() : List.<CommentDtos.ThreadMeta>of();
        var apiThreads = new ArrayList<CommentDtos.Thread>();

        for (CommentDtos.ThreadMeta t : threadsMeta) {
            var commentsApi = new ArrayList<CommentDtos.Comment>();
            for (CommentDtos.CommentMeta cm : t.comments()) {
                var jc = byId.get(cm.jiraCommentId());
                String text = jc != null ? jc.body() : "[comment unavailable]";
                String createdAt = cm.createdAt() != null
                        ? cm.createdAt()
                        : (jc != null ? jc.created() : null);
                String updatedAt = cm.updatedAt() != null
                        ? cm.updatedAt()
                        : (jc != null ? jc.updated() : null);

                commentsApi.add(new CommentDtos.Comment(
                        cm.id(),
                        cm.author(),
                        text,
                        cm.body(),
                        createdAt,
                        updatedAt
                ));
            }

            apiThreads.add(new CommentDtos.Thread(
                    t.id(),
                    t.caseId() != null ? t.caseId() : issueKey,
                    t.anchor(),
                    t.createdBy(),
                    t.createdAt(),
                    t.resolved(),
                    commentsApi
            ));
        }

        return new CommentDtos.FetchRes(apiThreads);
    }

    private String authorName(JiraModels.UserResponse me) {
        if (me == null) return "unknown";
        if (me.displayName() != null && !me.displayName().isBlank()) return me.displayName();
        if (me.name() != null && !me.name().isBlank()) return me.name();
        return me.key() != null ? me.key() : "unknown";
    }

    private String renderBody(CommentDtos.CommentAnchor anchor, String text, Object tiptapBody, boolean isReply) {
        return CommentsTipTapJiraWikiRenderer.render(tiptapBody, text);
    }
}
