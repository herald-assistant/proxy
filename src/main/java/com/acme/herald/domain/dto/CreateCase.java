package com.acme.herald.domain.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateCase(
        @NotBlank String case_id,
        @NotBlank String template_id,
        @NotBlank String summary,
        String description,
        List<String> labels,
        @NotNull JsonNode payload, // json
        @NotNull String casePayload // wikimarkup
) {}
