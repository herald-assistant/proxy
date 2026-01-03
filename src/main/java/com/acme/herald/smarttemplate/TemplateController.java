package com.acme.herald.smarttemplate;

import com.acme.herald.domain.dto.CreateTemplate;
import com.acme.herald.domain.dto.TemplateRef;
import com.acme.herald.web.dto.CommonDtos;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/{issueKey}/like")
    public ResponseEntity<Void> like(@PathVariable String issueKey, @RequestBody CommonDtos.LikeReq req) {
        return service.like(issueKey, req);
    }
}
