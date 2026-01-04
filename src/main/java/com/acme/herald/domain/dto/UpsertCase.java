package com.acme.herald.domain.dto;

import tools.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Schema(description = "Request payload for creating or updating (upsert) a Case.")
public record UpsertCase(

        @Schema(description = "Stable case identifier used as the logical key for upsert lookup.", example = "case-2026-000123")
        @NotBlank
        String caseId,

        @Schema(description = "Optional template identifier used for linking the Case to a Template in the Provider.", example = "tpl-risk-assessment-v1")
        String templateId,

        @Schema(description = "Case title displayed to users (used as issue summary in the Provider).", example = "Customer onboarding - exception handling")
        String summary,

        @Schema(description = "Optional case description.", example = "Investigate and document the exception path for onboarding flow.")
        String description,

        @Schema(description = "Labels/tags assigned to the case in the Provider.", example = "[\"herald\",\"case\",\"onboarding\"]")
        List<String> labels,

        @Schema(description = "Arbitrary case payload as JSON (e.g., editor document, structured data).", example = "{\"kind\":\"typedPage\",\"version\":1}")
        @NotNull
        JsonNode payload,

        @Schema(description = "Case body in Provider-specific wiki/markup format.", example = "h2. Steps\\n* Step 1\\n* Step 2")
        @NotNull
        String casePayload,

        @Schema(description = "Case status value validated against server configuration.", example = "open")
        String status
) {}
