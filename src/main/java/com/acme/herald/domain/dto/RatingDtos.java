package com.acme.herald.domain.dto;

import java.util.Map;

public class RatingDtos {
    public record RatingUpsertReq(String catId, Integer value) {}

    public record RatingFetchRes(Map<String, Integer> mine,
                                 Map<String, Summary> summary) {
        public record Summary(double avg, int count) {}
    }

    /** Wartość przechowywana w Jira Issue Property */
    public record RatingIssueProperty(
            String ratingId,
            Map<String, Map<String, Integer>> votes // userId -> (catId -> value)
    ) {}
}
