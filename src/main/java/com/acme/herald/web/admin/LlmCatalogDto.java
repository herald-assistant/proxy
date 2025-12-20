package com.acme.herald.web.admin;

import java.util.List;

public record LlmCatalogDto(
        Integer version,
        List<LlmCatalogModelDto> models
) {
}