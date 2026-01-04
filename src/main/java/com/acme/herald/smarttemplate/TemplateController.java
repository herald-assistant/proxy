package com.acme.herald.smarttemplate;

import com.acme.herald.domain.dto.TemplateRef;
import com.acme.herald.domain.dto.UpsertTemplate;
import com.acme.herald.web.dto.CommonDtos;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "/templates", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "TemplateController", description = "Smart Template operations exposed by the Provider proxy.")
public class TemplateController {

    private final TemplateService service;

    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Create or update a Smart Template",
            description = "Creates a new template or updates an existing one in the configured Provider and returns a reference to the underlying issue."
    )
    public ResponseEntity<TemplateRef> upsert(@RequestBody @Valid UpsertTemplate req) {
        var ref = service.upsertTemplate(req);
        return ResponseEntity.ok(ref);
    }

    @PutMapping(path = "/{issueKey}/like", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Like or unlike a template",
            description = "Sets the like state for the template represented by the given Provider issue key."
    )
    public ResponseEntity<Void> like(@PathVariable String issueKey, @RequestBody @Valid CommonDtos.LikeReq req) {
        service.like(issueKey, req);
        return ResponseEntity.noContent().build();
    }
}
