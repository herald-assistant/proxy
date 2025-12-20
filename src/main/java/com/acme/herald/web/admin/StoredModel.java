package com.acme.herald.web.admin;

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