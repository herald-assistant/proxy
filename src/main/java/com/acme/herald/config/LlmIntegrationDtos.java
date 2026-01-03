package com.acme.herald.config;

import java.util.List;

public class LlmIntegrationDtos {
    public record LlmCatalogDto(
            Integer version,
            List<LlmCatalogModelDto> models
    ) {}

    public record LlmCatalogModelDto(
            String id,
            String label,
            String model,
            String baseUrl,
            Integer contextWindowTokens,
            Boolean enabled,
            String notes,
            SupportsDto supports,
            DefaultsDto defaults,
            String token,
            Boolean tokenSet
    ) {}


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
            SupportsDto supports,
            DefaultsDto defaults,

            String tokenEnc
    ) {}

    public record SupportsDto(
            Boolean temperature,
            Boolean json
    ) {}

    public record DefaultsDto(Double temperature, Integer maxTokens) {}

}
