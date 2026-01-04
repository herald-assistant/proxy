package com.acme.herald.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@Schema(description = "Single search hit returned from the configured Provider.")
public record SearchItem(

        @Schema(description = "Provider issue key.", example = "ABC-123")
        String issueKey,

        @Schema(
                description = "Raw issue fields returned by the Provider. Structure depends on Provider configuration and permissions.",
                example = "{\"summary\":\"Case from HRLD-303\",\"labels\":[\"herald\",\"case\"],\"status\":{\"name\":\"Open\"}}"
        )
        Map<String, Object> fields
) {}
