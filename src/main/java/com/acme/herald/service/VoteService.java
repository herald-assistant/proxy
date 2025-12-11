// package com.acme.herald.service;
package com.acme.herald.service;

import com.acme.herald.domain.JiraModels;
import com.acme.herald.domain.dto.VoteDtos;
import com.acme.herald.provider.JiraProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class VoteService {
    private final JiraProvider jira;

    public VoteDtos.VoteFetchRes fetch(String issueKey, String voteId) {
        var me = jira.getMe();
        var userId = jiraUserId(me);

        var stored = readProperty(issueKey, voteId);
        var votes = stored.votes() == null ? Map.<String, String>of() : stored.votes();

        String mine = votes.get(userId);
        var summary = summarize(votes);
        return new VoteDtos.VoteFetchRes(mine, summary);
    }

    public VoteDtos.VoteFetchRes upsert(String issueKey, String voteId, String dir) {
        // dir: "up" | "down" | null
        if (dir != null && !(dir.equals("up") || dir.equals("down"))) {
            // cichy fallback; możesz rzucić 400
            dir = null;
        }

        var me = jira.getMe();
        var userId = jiraUserId(me);

        var stored = readProperty(issueKey, voteId);
        var votes = new HashMap<>(stored.votes() == null ? Map.of() : stored.votes());

        if (dir == null) votes.remove(userId);
        else votes.put(userId, dir);

        var next = new VoteDtos.VoteIssueProperty(voteId, votes);
        jira.setIssueProperty(issueKey, propertyKey(voteId), next);

        String mine = votes.get(userId);
        var summary = summarize(votes);
        return new VoteDtos.VoteFetchRes(mine, summary);
    }

    // ───── helpers ─────

    private VoteDtos.VoteIssueProperty readProperty(String issueKey, String voteId) {
        try {
            Map<String, Object> resp = jira.getIssueProperty(issueKey, propertyKey(voteId));
            Object val = resp.get("value"); // Jira: {"key": "...", "value": {...}}
            if (val instanceof Map valueMap) {
                @SuppressWarnings("unchecked")
                Map<String, Object> m = valueMap;
                String vId = (String) m.getOrDefault("voteId", voteId);
                @SuppressWarnings("unchecked")
                Map<String, String> votes = (Map<String, String>) m.getOrDefault("votes", Map.of());
                return new VoteDtos.VoteIssueProperty(vId, votes);
            }
        } catch (Exception ignored) {}
        return new VoteDtos.VoteIssueProperty(voteId, new HashMap<>());
    }

    private String propertyKey(String voteId) {
        var safe = (voteId == null ? "unnamed" : voteId.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9._-]", "_"));
        return "herald.vote." + safe;
    }

    private VoteDtos.VoteFetchRes.Summary summarize(Map<String, String> votes) {
        int up = 0, down = 0;
        for (String v : votes.values()) {
            if ("up".equals(v)) up++;
            else if ("down".equals(v)) down++;
        }
        int voters = votes.size();
        int score = up - down;
        return new VoteDtos.VoteFetchRes.Summary(up, down, voters, score);
    }

    private String jiraUserId(JiraModels.UserResponse me) {
        return (me != null && me.name() != null && !me.name().isBlank())
                ? me.name()
                : (me != null ? me.key() : "anonymous");
    }
}
