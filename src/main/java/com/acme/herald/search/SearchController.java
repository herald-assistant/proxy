package com.acme.herald.search;

import com.acme.herald.domain.dto.SearchResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "SearchController", description = "Search operations exposed by the Provider proxy.")
public class SearchController {
    private final SearchService service;

    @GetMapping
    @Operation(
            summary = "Search issues in the Provider",
            description = "Executes a query against the configured Provider and returns matching items."
    )
    public ResponseEntity<SearchResult> search(
            @Parameter(description = "Provider query string (syntax depends on Provider).", example = "project = ABC AND text ~ \"onboarding\"")
            @RequestParam(required = false) String q,

            @Parameter(description = "Maximum number of results to return.", example = "50")
            @RequestParam(defaultValue = "50") int limit
    ) {
        return ResponseEntity.ok(service.search(q, limit));
    }
}
