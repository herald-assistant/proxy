package com.acme.herald.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Search response containing a list of matching items.")
public record SearchResult(
        @Schema(description = "List of matched items.", example = "[{\"issueKey\":\"ABC-123\",\"fields\":{\"summary\":\"Example\"}}]")
        List<SearchItem> items
) {}
