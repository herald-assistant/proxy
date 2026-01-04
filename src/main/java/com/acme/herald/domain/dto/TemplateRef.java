package com.acme.herald.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Reference to a template stored as an issue in the configured Provider.")
public record TemplateRef(

        @Schema(description = "Provider issue key.",
                example = "ABC-123")
        String issueKey,

        @Schema(description = "Browser URL pointing to the Provider issue.",
                example = "https://provider.acme.com/browse/ABC-123")
        String url
) {}
