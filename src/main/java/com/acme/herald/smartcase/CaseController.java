package com.acme.herald.smartcase;

import com.acme.herald.domain.dto.CaseRef;
import com.acme.herald.domain.dto.UpsertCase;
import com.acme.herald.web.dto.CommonDtos;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "/cases", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "CaseController", description = "Smart Case operations exposed by the Provider proxy.")
public class CaseController {

    private final CaseService service;

    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Create or update a Smart Case",
            description = "Creates a new case or updates an existing one in the configured Provider and returns a reference to the underlying issue."
    )
    public ResponseEntity<CaseRef> upsertCase(@RequestBody @Valid UpsertCase req) {
        return ResponseEntity.ok(service.upsertCase(req));
    }

    @PutMapping(path = "/{issueKey}/like", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Like or unlike a Smart Case",
            description = "Sets the like state for the case represented by the given Provider issue key."
    )
    public ResponseEntity<Void> like(@PathVariable String issueKey, @RequestBody @Valid CommonDtos.LikeReq req) {
        service.like(issueKey, req.liked());
        return ResponseEntity.noContent().build();
    }
}
