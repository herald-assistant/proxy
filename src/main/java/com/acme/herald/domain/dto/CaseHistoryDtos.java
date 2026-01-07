package com.acme.herald.domain.dto;

import java.util.List;

public final class CaseHistoryDtos {
    private CaseHistoryDtos() {}

    public record PayloadVersion(
            String id,
            String createdAt,
            String author,
            String authorKey,
            String fromValue,
            String toValue,
            Integer size
    ) {}

    public record PayloadHistory(
            String issueKey,
            String fieldId,
            List<PayloadVersion> versions
    ) {}
}
