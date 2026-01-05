package com.acme.herald.config;

import com.acme.herald.config.LlmIntegrationDtos.LlmCatalogDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/admin/config", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(
        name = "LlmConfigController",
        description = "Admin endpoints for managing the LLM catalog used by the proxy."
)
public class LlmConfigController {

    private final LlmConfigService svc;

    @GetMapping("/llm-catalog")
    @Operation(
            summary = "Get LLM catalog",
            description = "Returns the current LLM catalog configuration used by the proxy. Secrets are never returned."
    )
    public LlmCatalogDto getLlmCatalog() {
        return svc.getCatalog();
    }

    @PutMapping(value = "/llm-catalog", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Upsert LLM catalog",
            description = "Stores the LLM catalog configuration used by the proxy. Token is write-only; when omitted, the previously stored token is preserved."
    )
    public ResponseEntity<Void> upsertLlmCatalog(@RequestBody @Valid LlmCatalogDto body) {
        svc.upsertLlmCatalog(body);
        return ResponseEntity.noContent().build();
    }
}
