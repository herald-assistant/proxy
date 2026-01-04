package com.acme.herald.domain.dto;

import tools.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Schema(description = "Request payload for creating or updating (upsert) a Smart Template.")
public record UpsertTemplate(

        @Schema(description = "Stable template identifier used as the logical key for upsert lookup.",
                example = "tpl-risk-assessment-v1")
        @NotBlank
        String templateId,

        @Schema(description = "Template title displayed to users (used as issue summary in the Provider).",
                example = "Release Risk Assessment (MVP)")
        @NotBlank
        String title,

        @Schema(description = "Labels/tags assigned to the template in the Provider.",
                example = "[\"herald\",\"template\",\"risk\"]")
        List<String> labels,

        @Schema(description = "Arbitrary template payload as JSON (e.g., editor document, slot configuration).",
                example = "{\"kind\":\"typedPage\",\"version\":1,\"sections\":[{\"id\":\"scope\",\"title\":\"Scope\"}]}")
        @NotNull
        JsonNode payload,

        @Schema(description = "Template status value validated against server configuration.",
                example = "draft")
        String status
) {}
