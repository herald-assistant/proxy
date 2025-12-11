package com.acme.herald.domain.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateTemplate(
        @NotBlank String template_id,
        @NotBlank String title,
        List<String> labels,
        @NotNull JsonNode payload
) {}
