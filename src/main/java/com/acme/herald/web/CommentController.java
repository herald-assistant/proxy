// package com.acme.herald.web;
package com.acme.herald.web;

import com.acme.herald.domain.dto.CommentDtos;
import com.acme.herald.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "/comments", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class CommentController {

    private final CommentService service;

    @GetMapping("/issues/{issueKey}")
    public ResponseEntity<CommentDtos.FetchRes> fetch(@PathVariable String issueKey) {
        return ResponseEntity.ok(service.fetch(issueKey));
    }

    @PostMapping(
            path = "/issues/{issueKey}/threads",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<CommentDtos.FetchRes> addRootComment(
            @PathVariable String issueKey,
            @RequestBody CommentDtos.AddRootCommentReq body
    ) {
        return ResponseEntity.ok(service.addRootComment(issueKey, body));
    }

    @PostMapping(
            path = "/issues/{issueKey}/threads/{threadId}/reply",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<CommentDtos.FetchRes> replyToThread(
            @PathVariable String issueKey,
            @PathVariable String threadId,
            @RequestBody CommentDtos.ReplyReq body
    ) {
        return ResponseEntity.ok(service.reply(issueKey, threadId, body));
    }

    @PutMapping(
            path = "/issues/{issueKey}/threads/{threadId}/comments/{commentId}",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<CommentDtos.FetchRes> editComment(
            @PathVariable String issueKey,
            @PathVariable String threadId,
            @PathVariable String commentId,
            @RequestBody CommentDtos.EditReq body
    ) {
        return ResponseEntity.ok(service.edit(issueKey, threadId, commentId, body));
    }

    @DeleteMapping(
            path = "/issues/{issueKey}/threads/{threadId}/comments/{commentId}"
    )
    public ResponseEntity<CommentDtos.FetchRes> deleteComment(
            @PathVariable String issueKey,
            @PathVariable String threadId,
            @PathVariable String commentId
    ) {
        return ResponseEntity.ok(service.delete(issueKey, threadId, commentId));
    }

    @PatchMapping(
            path = "/issues/{issueKey}/threads/{threadId}/resolve",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<CommentDtos.FetchRes> resolveThread(
            @PathVariable String issueKey,
            @PathVariable String threadId,
            @RequestBody CommentDtos.ResolveReq body
    ) {
        return ResponseEntity.ok(service.resolve(issueKey, threadId, body));
    }
}
