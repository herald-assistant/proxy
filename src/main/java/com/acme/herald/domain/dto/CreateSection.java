package com.acme.herald.domain.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateSection(
        @NotBlank String section_id,
        @NotBlank String title,
        String assignee
) {}
