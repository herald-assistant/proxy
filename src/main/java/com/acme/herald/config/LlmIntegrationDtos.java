package com.acme.herald.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public final class LlmIntegrationDtos {
    private LlmIntegrationDtos() {}

    @Schema(description = "LLM catalog configuration used by the proxy.")
    public record LlmCatalogDto(

            @Schema(description = "Configuration schema version.", example = "1")
            Integer version,

            @Schema(description = "List of catalog models available to the proxy.", example = "[]")
            @Valid
            List<LlmCatalogModelDto> models
    ) {}

    @Schema(description = "A single LLM model entry in the catalog.")
    public record LlmCatalogModelDto(

            @Schema(description = "Stable catalog identifier used by clients (selection key).", example = "gpt-4o")
            @NotBlank
            String id,

            @Schema(description = "Human-friendly label displayed in UI.", example = "GPT-4o (fast)")
            String label,

            @Schema(description = "Provider model name (runtime parameter passed to the Provider).", example = "gpt-4o")
            String model,

            @Schema(description = "Optional base URL override for the Provider endpoint.", example = "https://llm-gateway.company.local")
            String baseUrl,

            @Schema(description = "Context window size in tokens (used for UI hints and guardrails).", example = "128000")
            Integer contextWindowTokens,

            @Schema(description = "Whether the model is enabled and selectable in the proxy.", example = "true")
            Boolean enabled,

            @Schema(description = "Optional free-form notes for admins.", example = "Use for interactive chat; best latency.")
            String notes,

            @Schema(description = "Feature support flags for this model.", example = "{\"chat\":true,\"toolCalls\":true,\"json\":true,\"vision\":false,\"embeddings\":false}")
            @Valid
            LlmModelSupportsDto supports,

            @Schema(description = "Default inference parameters applied by the proxy when not provided by the client.", example = "{\"temperature\":0.2,\"maxTokens\":9600}")
            @Valid
            LlmModelDefaultsDto defaults,

            @Schema(description = "Write-only access token for the Provider. If omitted or blank, the previous token is kept.", example = "sk-live-***")
            @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
            String token,

            @Schema(description = "Indicates whether an encrypted token is already stored for this model.", example = "true")
            Boolean tokenPresent
    ) {}

    @Schema(description = "Capability flags supported by a specific model.")
    public record LlmModelSupportsDto(

            @Schema(description = "Supports chat-style completion.", example = "true")
            Boolean chat,

            @Schema(description = "Supports tool/function calls.", example = "true")
            Boolean toolCalls,

            @Schema(description = "Supports structured JSON output mode (or reliable JSON generation).", example = "true")
            Boolean json,

            @Schema(description = "Supports vision/image input.", example = "false")
            Boolean vision,

            @Schema(description = "Supports embeddings generation.", example = "false")
            Boolean embeddings
    ) {}

    @Schema(description = "Default inference parameters for a model.")
    public record LlmModelDefaultsDto(

            @Schema(description = "Default sampling temperature.", example = "0.2")
            Double temperature,

            @Schema(description = "Default max tokens for the response.", example = "9600")
            Integer maxTokens
    ) {}

    // ─────────────────────────────────────────────────────────────
    // Internal storage models (not required in OpenAPI, but convenient
    // to keep in the same file). No @Schema needed.
    // ─────────────────────────────────────────────────────────────

    public record StoredCatalog(
            Integer version,
            List<StoredModel> models
    ) {}

    public record StoredModel(
            String id,
            String label,
            String model,
            String baseUrl,
            Integer contextWindowTokens,
            Boolean enabled,
            String notes,
            LlmModelSupportsDto supports,
            LlmModelDefaultsDto defaults,
            String tokenEnc
    ) {}
}
