package com.acme.herald.web.admin;

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