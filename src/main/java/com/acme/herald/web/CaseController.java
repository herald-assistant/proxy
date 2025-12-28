package com.acme.herald.web;

import com.acme.herald.domain.dto.CaseRef;
import com.acme.herald.domain.dto.CreateCase;
import com.acme.herald.domain.dto.RatingInput;
import com.acme.herald.domain.dto.RatingResult;
import com.acme.herald.service.CaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "/cases", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class CaseController {
    private final CaseService service;

    @PostMapping
    public ResponseEntity<CaseRef> upsertCase(@RequestBody @Valid CreateCase req) {
        return ResponseEntity.status(201).body(service.upsertCase(req));
    }

    @PostMapping("/{caseKey}/comments")
    public ResponseEntity<Void> comment(@PathVariable String caseKey, @RequestBody CommentReq req) {
        service.commentWithMentions(caseKey, req.text());
        return ResponseEntity.status(201).build();
    }

    @PostMapping("/{caseKey}/vote")
    public ResponseEntity<Void> vote(@PathVariable String caseKey, @RequestBody VoteReq req) {
        service.vote(caseKey, req.up());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{caseKey}/rating")
    public ResponseEntity<RatingResult> rate(@PathVariable String caseKey, @RequestBody RatingInput req) {
        return ResponseEntity.ok(service.rate(caseKey, req));
    }

    public record TransitionReq(String transitionId) {
    }

    public record CommentReq(String text) {
    }

    public record VoteReq(boolean up) {
    }
}
