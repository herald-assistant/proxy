package com.acme.herald.comment;

import com.acme.herald.domain.dto.CommentDtos;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "/comments", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "CommentController", description = "Threaded comment operations exposed by the Provider proxy.")
public class CommentController {

    private final CommentService service;

    @GetMapping("/issues/{issueKey}")
    @Operation(
            summary = "Fetch comment threads for an issue",
            description = "Returns all comment threads stored for the given Provider issue key."
    )
    public ResponseEntity<CommentDtos.FetchRes> fetch(
            @Parameter(description = "Provider issue key.", example = "ABC-123")
            @PathVariable String issueKey
    ) {
        return ResponseEntity.ok(service.fetch(issueKey));
    }

    @PostMapping(path = "/issues/{issueKey}/threads", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Create a new root comment thread",
            description = "Creates a new comment thread for the issue. The first comment becomes the root of the thread."
    )
    public ResponseEntity<CommentDtos.FetchRes> addRootComment(
            @Parameter(description = "Provider issue key.", example = "ABC-123")
            @PathVariable String issueKey,
            @RequestBody @Valid CommentDtos.AddRootCommentReq body
    ) {
        return ResponseEntity.ok(service.addRootComment(issueKey, body));
    }

    @PostMapping(path = "/issues/{issueKey}/threads/{threadId}/reply", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Reply to an existing thread",
            description = "Adds a new comment to the specified thread."
    )
    public ResponseEntity<CommentDtos.FetchRes> replyToThread(
            @Parameter(description = "Provider issue key.", example = "ABC-123")
            @PathVariable String issueKey,
            @Parameter(description = "Thread identifier.", example = "t-7c7c2b64")
            @PathVariable String threadId,
            @RequestBody @Valid CommentDtos.ReplyReq body
    ) {
        return ResponseEntity.ok(service.reply(issueKey, threadId, body));
    }

    @PutMapping(path = "/issues/{issueKey}/threads/{threadId}/comments/{commentId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Edit a comment",
            description = "Updates the content of an existing comment within a thread."
    )
    public ResponseEntity<CommentDtos.FetchRes> editComment(
            @Parameter(description = "Provider issue key.", example = "ABC-123")
            @PathVariable String issueKey,
            @Parameter(description = "Thread identifier.", example = "t-7c7c2b64")
            @PathVariable String threadId,
            @Parameter(description = "Herald comment identifier.", example = "c-3f6d5a1e")
            @PathVariable String commentId,
            @RequestBody @Valid CommentDtos.EditReq body
    ) {
        return ResponseEntity.ok(service.edit(issueKey, threadId, commentId, body));
    }

    @DeleteMapping(path = "/issues/{issueKey}/threads/{threadId}/comments/{commentId}")
    @Operation(
            summary = "Delete a comment",
            description = "Deletes a comment from a thread. If it was the last comment in the thread, the thread is removed."
    )
    public ResponseEntity<CommentDtos.FetchRes> deleteComment(
            @Parameter(description = "Provider issue key.", example = "ABC-123")
            @PathVariable String issueKey,
            @Parameter(description = "Thread identifier.", example = "t-7c7c2b64")
            @PathVariable String threadId,
            @Parameter(description = "Herald comment identifier.", example = "c-3f6d5a1e")
            @PathVariable String commentId
    ) {
        return ResponseEntity.ok(service.delete(issueKey, threadId, commentId));
    }

    @PatchMapping(path = "/issues/{issueKey}/threads/{threadId}/resolve", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Resolve or reopen a thread",
            description = "Marks the thread as resolved or unresolved."
    )
    public ResponseEntity<CommentDtos.FetchRes> resolveThread(
            @Parameter(description = "Provider issue key.", example = "ABC-123")
            @PathVariable String issueKey,
            @Parameter(description = "Thread identifier.", example = "t-7c7c2b64")
            @PathVariable String threadId,
            @RequestBody @Valid CommentDtos.ResolveReq body
    ) {
        return ResponseEntity.ok(service.resolve(issueKey, threadId, body));
    }
}
