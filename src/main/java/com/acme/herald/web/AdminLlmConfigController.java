package com.acme.herald.web;

import com.acme.herald.service.AdminLlmConfigService;
import com.acme.herald.web.admin.LlmIntegrationDtos.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/admin/config", produces = MediaType.APPLICATION_JSON_VALUE)
public class AdminLlmConfigController {

    private final AdminLlmConfigService svc;

    @GetMapping("/llm-catalog")
    public LlmCatalogDto getLlmCatalog() {
        return svc.getCatalog();
    }

    @PutMapping(value = "/llm-catalog", consumes = MediaType.APPLICATION_JSON_VALUE)
    public void upsertLlmCatalog(@RequestBody LlmCatalogDto body) {
        svc.upsertLlmCatalog(body);
    }
}
