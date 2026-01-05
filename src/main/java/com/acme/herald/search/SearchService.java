package com.acme.herald.search;

import com.acme.herald.domain.JiraModels;
import com.acme.herald.domain.dto.SearchItem;
import com.acme.herald.domain.dto.SearchResult;
import com.acme.herald.provider.JiraProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchService {
    private final JiraProvider jira;

    public SearchResult search(String jql, int limit) {
        JiraModels.SearchResponse resp = jira.search(jql, 0, limit);

        List<SearchItem> items = new ArrayList<>();
        var issues = resp.issues();
        if (issues != null) {
            for (var i : issues) {
                String key = i.path("key").asText(null);
                var fields = i.path("fields");
                items.add(new SearchItem(key, fields));
            }
        }

        return new SearchResult(items);
    }
}
