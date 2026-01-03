package com.acme.herald.smarttemplate;

import com.acme.herald.domain.dto.CreateTemplate;
import com.acme.herald.domain.dto.TemplateRef;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/templates", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class TemplateController {
    private final TemplateService service;

    @PostMapping
    public ResponseEntity<TemplateRef> create(@RequestBody @Valid CreateTemplate req) {
        var ref = service.createTemplate(req);
        return ResponseEntity.status(201).body(ref);
    }

}
