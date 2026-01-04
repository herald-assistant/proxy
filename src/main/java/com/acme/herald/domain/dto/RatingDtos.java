package com.acme.herald.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

public class RatingDtos {

    @Schema(description = "Request payload for creating or updating a rating value for a category.")
    public record RatingUpsertReq(
            @Schema(description = "Rating category identifier.", example = "readability")
            String catId,

            @Schema(description = "Rating value for the given category. Use null to remove the rating.", example = "4")
            Integer value
    ) {}

    @Schema(description = "Rating fetch response: user's own ratings and aggregated summary across all users.")
    public record RatingFetchRes(

            @Schema(
                    description = "Ratings submitted by the current user (categoryId -> value).",
                    example = "{\"readability\":4,\"correctness\":5}"
            )
            Map<String, Integer> mine,

            @Schema(
                    description = "Aggregated summary across all users (categoryId -> summary).",
                    example = "{\"readability\":{\"avg\":4.2,\"count\":12},\"correctness\":{\"avg\":4.8,\"count\":10}}"
            )
            Map<String, Summary> summary
    ) {
        @Schema(description = "Aggregated rating statistics for a single category.")
        public record Summary(
                @Schema(description = "Average value across all users.", example = "4.2")
                double avg,

                @Schema(description = "Number of submitted ratings.", example = "12")
                int count
        ) {}
    }

    @Schema(description = "Internal storage model for a rating stored in a Provider issue property.")
    public record RatingIssueProperty(
            @Schema(description = "Rating identifier.", example = "quality-v1")
            String ratingId,

            @Schema(
                    description = "Votes stored as userId -> (categoryId -> value).",
                    example = "{\"alice\":{\"readability\":4},\"bob\":{\"readability\":5,\"correctness\":5}}"
            )
            Map<String, Map<String, Integer>> votes
    ) {}
}
