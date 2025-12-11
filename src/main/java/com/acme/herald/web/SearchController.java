package com.acme.herald.web;

import com.acme.herald.domain.dto.SearchResult;
import com.acme.herald.service.SearchService;
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
public class SearchController {
    private final SearchService service;

    @GetMapping
    public ResponseEntity<SearchResult> search(@RequestParam(required = false) String q,
                                               @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(service.search(q, limit));
    }
}
