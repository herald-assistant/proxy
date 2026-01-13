package com.acme.herald.links;

import com.acme.herald.domain.JiraModels;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(path = "/links", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "LinkController", description = "Helper endpoints for link related data (e.g. issue link types).")
public class LinkController {

    private final LinkService service;

    @GetMapping("/issue-link-types")
    @Operation(
            summary = "List Jira issue link types",
            description = "Returns available issue link types (name/inward/outward) used to configure template relations."
    )
    public ResponseEntity<List<JiraModels.IssueLinkType>> list() {
        return ResponseEntity.ok(service.list());
    }
}
