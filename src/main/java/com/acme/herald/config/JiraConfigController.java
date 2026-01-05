package com.acme.herald.config;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.acme.herald.config.JiraIntegrationConfigDtos.JiraIntegrationConfigDto;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/admin/jira", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(
        name = "AdminJiraConfigController",
        description = "Admin endpoints for managing Provider integration configuration used by the proxy."
)
public class JiraConfigController {

    private final AdminJiraConfigService service;

    @GetMapping
    @Operation(
            summary = "Get Provider integration configuration",
            description = "Returns the current integration configuration used by the proxy at runtime."
    )
    public JiraIntegrationConfigDto getJiraConfig() {
        return service.getForRuntime();
    }

    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Upsert Provider integration configuration",
            description = "Stores integration configuration used by the proxy. Requires project admin privileges."
    )
    public ResponseEntity<Void> upsertJiraConfig(@RequestBody @Valid JiraIntegrationConfigDto dto) {
        service.saveForAdmin(dto);
        return ResponseEntity.noContent().build();
    }
}
