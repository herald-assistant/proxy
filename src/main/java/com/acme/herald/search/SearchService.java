package com.acme.herald.search;

import com.acme.herald.config.JiraConfigService;
import com.acme.herald.config.JiraIntegrationConfigDtos;
import com.acme.herald.domain.JiraModels;
import com.acme.herald.domain.dto.SearchItem;
import com.acme.herald.domain.dto.SearchResult;
import com.acme.herald.provider.JiraProvider;
import com.acme.herald.web.JqlUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SearchService {
    private final JiraProvider jira;
    private final JiraConfigService jiraCfg;

    // W polach trzymasz kategorię, więc to jest stałe i stabilne.
    private static final String REJECTED = "REJECTED";

    public SearchResult search(String query, int limit) {
        var cfg = jiraCfg.getForRuntime();

        String guardedQuery = addNotRejectedGuard(query, cfg);

        JiraModels.SearchResponse resp = jira.search(guardedQuery, 0, limit);

        List<SearchItem> items = new ArrayList<>();
        var issues = resp.issues();
        if (issues != null) {
            for (var i : issues) {
                String key = i.path("key").asString(null);
                var fields = i.path("fields");
                items.add(new SearchItem(key, fields));
            }
        }
        return new SearchResult(items);
    }

    /**
     * Dokleja do dowolnego JQL-a filtr ukrywający REJECTED w polach statusowych.
     * Wstrzykuje warunek przed ORDER BY (jeśli istnieje).
     */
    private String addNotRejectedGuard(String rawQuery, JiraIntegrationConfigDtos.JiraIntegrationConfigDto cfg) {
        String base = (rawQuery == null) ? "" : rawQuery.trim();
        String guard = buildNotRejectedGuard(cfg).trim();
        if (guard.isEmpty()) return base;

        SplitOrderBy split = splitOrderBy(base);

        String head = split.head.trim();
        String orderBy = split.orderBy.trim();

        String out;
        if (head.isEmpty()) {
            // guard zaczyna się od AND — przy pustym query trzeba to zdjąć
            out = stripLeadingAnd(guard);
        } else {
            out = head + " " + ensureLeadingAnd(guard);
        }

        if (!orderBy.isEmpty()) out = out + " " + orderBy;
        return out.trim();
    }

    /**
     * Buduje fragment:
     * AND (
     *    (templateStatus is EMPTY OR templateStatus !~ "REJECTED")
     *    AND
     *    (caseStatus is EMPTY OR caseStatus !~ "REJECTED")
     * )
     *
     * Jeśli oba pola są takie same, warunek pojawi się tylko raz.
     */
    private String buildNotRejectedGuard(JiraIntegrationConfigDtos.JiraIntegrationConfigDto cfg) {
        if (cfg == null || cfg.fields() == null) return "";

        // Zbierz unikalne pola statusowe (caseStatus i templateStatus mogą być takie same)
        Set<String> jqlFields = new LinkedHashSet<>();

        String tplFieldRaw = nz(cfg.fields().templateStatus());
        String caseFieldRaw = nz(cfg.fields().caseStatus());

        if (!tplFieldRaw.isBlank()) jqlFields.add(JqlUtils.toJqlField(tplFieldRaw));
        if (!caseFieldRaw.isBlank()) jqlFields.add(JqlUtils.toJqlField(caseFieldRaw));

        if (jqlFields.isEmpty()) return "";

        List<String> parts = new ArrayList<>();
        for (String f : jqlFields) {
            parts.add("(" + f + " is EMPTY OR " + f + " !~ " + jqlQuote(REJECTED) + ")");
        }

        return "AND (" + String.join(" AND ", parts) + ")";
    }

    private static String nz(String s) {
        return s == null ? "" : s.trim();
    }

    private static String jqlQuote(String v) {
        // Jira JQL string literal: "..." (escape backslash and quote)
        String s = v == null ? "" : v;
        s = s.replace("\\", "\\\\").replace("\"", "\\\"");
        return "\"" + s + "\"";
    }

    private static String ensureLeadingAnd(String guard) {
        String g = guard.trim();
        if (g.isEmpty()) return g;
        return startsWithIgnoreCase(g, "AND ") ? g : "AND " + g;
    }

    private static String stripLeadingAnd(String guard) {
        String g = guard.trim();
        if (startsWithIgnoreCase(g, "AND ")) return g.substring(3).trim();
        return g;
    }

    private static boolean startsWithIgnoreCase(String s, String prefix) {
        if (s.length() < prefix.length()) return false;
        return s.regionMatches(true, 0, prefix, 0, prefix.length());
    }

    private static SplitOrderBy splitOrderBy(String q) {
        if (q == null) return new SplitOrderBy("", "");
        int idx = indexOfIgnoreCase(q, " order by ");
        if (idx < 0) return new SplitOrderBy(q, "");
        return new SplitOrderBy(q.substring(0, idx), q.substring(idx).trim());
    }

    private static int indexOfIgnoreCase(String s, String needle) {
        if (s == null || needle == null) return -1;
        final int n = s.length();
        final int m = needle.length();
        if (m == 0) return 0;
        for (int i = 0; i <= n - m; i++) {
            if (s.regionMatches(true, i, needle, 0, m)) return i;
        }
        return -1;
    }

    private record SplitOrderBy(String head, String orderBy) {}
}
