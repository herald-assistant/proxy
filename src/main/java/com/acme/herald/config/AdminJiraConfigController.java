package com.acme.herald.config;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.acme.herald.config.JiraIntegrationConfigDtos.JiraIntegrationConfigDto;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/admin/jira", produces = MediaType.APPLICATION_JSON_VALUE)
public class AdminJiraConfigController {

    private final AdminJiraConfigService service;

    @GetMapping
    public JiraIntegrationConfigDto getJiraConfig() {
        return service.getForAdmin();
    }

    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public void upsertJiraConfig(@RequestBody JiraIntegrationConfigDto dto) {
        service.saveForAdmin(dto);
    }
}
