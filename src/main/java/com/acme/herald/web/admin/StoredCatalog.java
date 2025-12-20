package com.acme.herald.web.admin;

import java.util.List;

public record StoredCatalog(
        Integer version,
        List<StoredModel> models
) {}