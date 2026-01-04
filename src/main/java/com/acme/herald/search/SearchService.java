package com.acme.herald.search;

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

    public SearchResult search(String query, int limit) {
        var resp = jira.search(query, 0, limit);
        var items = resp.issues().stream().map(i -> {
            String key = (String) i.get("key");
            Map<String, Object> fields = (Map<String, Object>) i.get("fields");
            return new SearchItem(key, fields);
        }).toList();
        return new SearchResult(items);
    }
}