package com.acme.herald.web;

import com.acme.herald.domain.JiraModels;
import com.acme.herald.domain.dto.CreateTemplate;
import com.acme.herald.domain.dto.TemplateRef;
import com.acme.herald.service.TemplateService;
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

    @PostMapping("/{caseKey}/transition")
    public ResponseEntity<Void> templateTransition(@PathVariable String caseKey, @RequestBody CaseController.TransitionReq req) {
        service.transition(caseKey, req.transitionId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{caseKey}/transitions")
    public JiraModels.TransitionList templateTransitions(@PathVariable String caseKey) {
        return service.transitions(caseKey);
    }
}
