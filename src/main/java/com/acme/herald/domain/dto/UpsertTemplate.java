package com.acme.herald.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import tools.jackson.databind.JsonNode;

import java.util.List;

public record UpsertTemplate(
        @NotBlank String template_id,
        @NotBlank String title,
        List<String> labels,
        @NotNull JsonNode payload,
        String status
) {
}
