package com.acme.herald.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class ChallengeDtos {
    private ChallengeDtos() {}

    @Schema(description = "Challenge shown in Template Hub.")
    public record Challenge(
            @Schema(example = "ch_9f3a2b1c") String id,
            @Schema(example = "Template do analizy driftu konfiguracji") String label,
            @Schema(description = "ISO timestamp/date", example = "2026-02-15") String deadline,
            @Schema(example = "Potrzebujemy template’u, który…") String description,

            @Schema(description = "Author technical key (derived from Jira /me).", example = "jdoe") String authorKey,
            @Schema(example = "Jan Doe") String authorDisplayName,

            @Schema(example = "2026-01-22T10:11:12Z") String createdAt,
            @Schema(example = "2026-01-22T10:11:12Z") String updatedAt
    ) {}

    public record ChallengeList(
            List<Challenge> challenges
    ) {}

    public record CreateChallengeReq(
            @NotBlank
            @Size(max = 140)
            String label,

            @Size(max = 4000)
            String description,

            @Schema(description = "ISO date or timestamp", example = "2026-02-15")
            String deadline
    ) {}

    public record UpdateChallengeReq(
            @Size(max = 140)
            String label,

            @Size(max = 4000)
            String description,

            @Schema(description = "ISO date or timestamp", example = "2026-02-15")
            String deadline
    ) {}
}
