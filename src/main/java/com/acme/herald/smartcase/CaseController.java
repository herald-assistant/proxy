package com.acme.herald.smartcase;

import com.acme.herald.domain.dto.CaseRef;
import com.acme.herald.domain.dto.UpsertCase;
import com.acme.herald.web.dto.CommonDtos;
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

    @PutMapping
    public ResponseEntity<CaseRef> upsertCase(@RequestBody @Valid UpsertCase req) {
        return ResponseEntity.status(201).body(service.upsertCase(req));
    }

    @PutMapping("/{issueKey}/like")
    public ResponseEntity<Void> like(@PathVariable String issueKey, @RequestBody CommonDtos.LikeReq req) {
        service.like(issueKey, req.liked());
        return ResponseEntity.noContent().build();
    }
}
