package com.acme.herald.domain;

import java.util.List;

public class HeraldConfigModels {

    public record LlmCatalog(
            int version,
            List<LlmCatalogModel> models
    ) {}

    public record LlmCatalogModel(
            String id,
            String label,
            String model,
            String baseUrl,
            Integer contextWindowTokens,
            boolean enabled,
            String notes,
            Supports supports,
            Defaults defaults,
            String token,
            Boolean tokenSet
    ) {}

    public record Supports(
            Boolean temperature,
            Boolean json
    ) {}

    public record Defaults(
            Double temperature,
            Integer maxTokens
    ) {}
}
