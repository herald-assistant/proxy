package com.acme.herald.service;

import com.acme.herald.domain.dto.SearchItem;
import com.acme.herald.domain.dto.SearchResult;
import com.acme.herald.provider.JiraProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class SearchService {
    private final JiraProvider jira;

    public SearchResult search(String q, int limit) {
        String jql = translateQToJql(q);
        var resp = jira.search(jql, 0, limit);
        var items = resp.issues().stream().map(i -> {
            String key = (String) i.get("key");
            Map<String, Object> fields = (Map<String, Object>) i.get("fields");
            return new SearchItem(key, fields);
        }).toList();
        return new SearchResult(items);
    }

    private String translateQToJql(String q) {
        if (q == null || q.isBlank())
            return "project = " + "HRLD"; // Minimalny parser: "template_id:XYZ AND status:Open"
        return q.replace("template_id:", "\"herald_template_id\" ~ ").replace("status:", "status = ");
    }
}