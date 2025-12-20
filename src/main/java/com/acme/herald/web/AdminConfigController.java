package com.acme.herald.web;

import com.acme.herald.service.AdminConfigService;
import com.acme.herald.web.admin.LlmCatalogDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/admin/config", produces = MediaType.APPLICATION_JSON_VALUE)
public class AdminConfigController {

    private final AdminConfigService svc;

    @GetMapping("/llm-catalog")
    public LlmCatalogDto get() {
        return svc.getCatalog();
    }

    @PutMapping(value = "/llm-catalog", consumes = MediaType.APPLICATION_JSON_VALUE)
    public void put(@RequestBody LlmCatalogDto body) {
        svc.saveCatalog(body);
    }
}
