package com.acme.herald.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

public final class VoteDtos {

    @Schema(description = "POST body for upserting a vote. Use dir=null to remove your vote.")
    public record VoteUpsertReq(
            @Schema(
                    description = "Vote direction. Allowed: 'up', 'down'. Null removes the user's vote.",
                    example = "up",
                    allowableValues = {"up", "down"}
            )
            String dir
    ) {}

    @Schema(description = "Vote fetch/upsert response containing the current user's vote and aggregated summary.")
    public record VoteFetchRes(
            @Schema(
                    description = "Current user's vote direction. Null means the user has not voted.",
                    example = "up",
                    allowableValues = {"up", "down"}
            )
            String mine,

            @Schema(description = "Aggregated vote summary for this voteId.")
            Summary summary
    ) {
        @Schema(description = "Aggregated vote statistics.")
        public record Summary(
                @Schema(description = "Number of 'up' votes.", example = "12")
                int up,

                @Schema(description = "Number of 'down' votes.", example = "3")
                int down,

                @Schema(description = "Total number of voters (unique users).", example = "15")
                int voters,

                @Schema(description = "Score computed as up - down.", example = "9")
                int score
        ) {}
    }

    @Schema(description = "Value stored in Provider (Jira) issue property for votes. Internal storage model.")
    public record VoteIssueProperty(
            @Schema(description = "Vote widget identifier.", example = "vote-overall")
            String voteId,

            @Schema(description = "Map of userId -> vote direction ('up'|'down').")
            Map<String, String> votes
    ) {}
}
