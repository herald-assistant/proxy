package com.acme.herald.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Reference to a case stored as an issue in the configured Provider.")
public record CaseRef(
        @Schema(description = "Provider issue key.", example = "ABC-456")
        String issueKey
) {}