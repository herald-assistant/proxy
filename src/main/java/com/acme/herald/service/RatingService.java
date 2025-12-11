package com.acme.herald.service;

import com.acme.herald.domain.JiraModels;
import com.acme.herald.domain.dto.RatingDtos.RatingFetchRes;
import com.acme.herald.domain.dto.RatingDtos.RatingIssueProperty;
import com.acme.herald.provider.JiraProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class RatingService {
    private final JiraProvider jira;

    public RatingFetchRes fetch(String issueKey, String ratingId) {
        var me = jira.getMe();
        var userId = jiraUserId(me);

        var stored = readProperty(issueKey, ratingId);
        var votes = stored.votes() == null ? Map.<String, Map<String, Integer>>of() : stored.votes();

        var mine = votes.getOrDefault(userId, Map.of());
        var summary = summarize(votes);
        return new RatingFetchRes(mine, summary);
    }

    public RatingFetchRes upsert(String issueKey, String ratingId, String catId, Integer value) {
        var me = jira.getMe();
        var userId = jiraUserId(me);

        var stored = readProperty(issueKey, ratingId);
        var votes = new HashMap<>(stored.votes() == null ? Map.of() : stored.votes());
        var userMap = new HashMap<>(votes.getOrDefault(userId, Map.of()));

        if (value == null) userMap.remove(catId);
        else userMap.put(catId, value);

        if (userMap.isEmpty()) votes.remove(userId);
        else votes.put(userId, userMap);

        var next = new RatingIssueProperty(ratingId, votes);
        jira.setIssueProperty(issueKey, propertyKey(ratingId), next); // body = raw JSON

        var summary = summarize(votes);
        var mine = votes.getOrDefault(userId, Map.of());
        return new RatingFetchRes(mine, summary);
    }

    // ───────── helpers ─────────

    private RatingIssueProperty readProperty(String issueKey, String ratingId) {
        try {
            Map<String, Object> resp = jira.getIssueProperty(issueKey, propertyKey(ratingId));
            // Jira zwraca {"key":"...","value":{...}} – interesuje nas pole "value".
            Object val = resp.get("value");
            if (val instanceof Map valueMap) {
                // prosta, bezpieczna deserializacja
                @SuppressWarnings("unchecked")
                Map<String, Object> m = valueMap;
                String rId = (String) m.getOrDefault("ratingId", ratingId);
                @SuppressWarnings("unchecked")
                Map<String, Map<String, Integer>> votes =
                        (Map<String, Map<String, Integer>>) m.getOrDefault("votes", Map.of());
                return new RatingIssueProperty(rId, votes);
            }
        } catch (Exception ignored) {}
        return new RatingIssueProperty(ratingId, new HashMap<>());
    }

    private String propertyKey(String ratingId) {
        // Jira property key: [a-zA-Z0-9-_ .] – robimy bezpieczny prefix i normalizację
        var safe = ratingId == null ? "unnamed" : ratingId.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9._-]", "_");
        return "herald.rating." + safe;
    }

    private Map<String, RatingFetchRes.Summary> summarize(Map<String, Map<String, Integer>> votes) {
        Map<String, Integer> counts = new HashMap<>();
        Map<String, Integer> sums = new HashMap<>();
        votes.values().forEach(catMap -> {
            catMap.forEach((cat, val) -> {
                if (val == null) return;
                counts.merge(cat, 1, Integer::sum);
                sums.merge(cat, val, Integer::sum);
            });
        });

        Map<String, RatingFetchRes.Summary> out = new HashMap<>();
        sums.forEach((cat, sum) -> {
            int c = counts.getOrDefault(cat, 0);
            double avg = c > 0 ? (sum * 1.0) / c : 0.0;
            out.put(cat, new RatingFetchRes.Summary(avg, c));
        });
        return out;
    }

    private String jiraUserId(JiraModels.UserResponse me) {
        // DC/Server: często "name" → unikalny login; fallback do "key"
        return (me != null && me.name() != null && !me.name().isBlank())
                ? me.name()
                : (me != null ? me.key() : "anonymous");
    }
}
