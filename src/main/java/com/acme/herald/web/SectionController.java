package com.acme.herald.web;

import com.acme.herald.domain.dto.CreateSection;
import com.acme.herald.domain.dto.SectionRef;
import com.acme.herald.service.SectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "/cases/{caseKey}/sections", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class SectionController {
    private final SectionService service;

    @PostMapping
    public ResponseEntity<SectionRef> create(@PathVariable String caseKey, @RequestBody @Valid CreateSection req) {
        return ResponseEntity.status(201).body(service.createSection(caseKey, req));
    }
}
