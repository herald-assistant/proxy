package com.acme.herald.domain.dto;


import java.util.Map;

public final class VoteDtos {
    /** GET/POST response */
    public record VoteFetchRes(String mine, Summary summary) {
        public record Summary(int up, int down, int voters, int score) {}
    }

    /** POST body: dir = "up" | "down" | null (null = usuń mój głos) */
    public record VoteUpsertReq(String dir) {}

    /** To, co trzymamy w Jira Issue Property */
    public record VoteIssueProperty(String voteId, Map<String, String> votes) {}
}