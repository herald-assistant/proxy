package com.acme.herald.web.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public final class CommonDtos {
    private CommonDtos() {}

    @Schema(description = "Request payload for setting the like state (like / unlike).")
    public record LikeReq(
            @Schema(description = "true = like, false = unlike.", example = "true")
            @NotNull
            @JsonAlias("up") // backward-compatibility for older clients
            Boolean liked
    ) {}

    @Schema(description = "Standard API error response payload.")
    public record ApiError(
            @Schema(description = "Application-level error code.", example = "VALIDATION_ERROR")
            String code,

            @Schema(description = "Human-readable error message.", example = "Invalid request payload.")
            String message,

            @Schema(description = "Optional list of per-field validation errors.", example = "[{\"field\":\"templateId\",\"error\":\"must not be blank\"}]")
            List<FieldError> fieldErrors,

            @Schema(description = "Optional trace identifier for correlating logs/telemetry.", example = "3f2a1c9d8b...")
            String traceId
    ) {}

    @Schema(description = "Validation error for a specific request field.")
    public record FieldError(
            @Schema(description = "Field name that failed validation.", example = "templateId")
            String field,

            @Schema(description = "Validation error message for the field.", example = "must not be blank")
            String error
    ) {}
}
