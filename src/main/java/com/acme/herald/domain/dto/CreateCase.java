package com.acme.herald.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import tools.jackson.databind.JsonNode;

import java.util.List;

public record CreateCase(
        @NotBlank String case_id,
        String template_id,
        String summary,
        String description,
        List<String> labels,
        @NotNull JsonNode payload, // json
        @NotNull String casePayload, // wikimarkup
        String status
) {}
