package com.acme.herald.smartcase;

import com.acme.herald.config.AdminJiraConfigService;
import com.acme.herald.domain.JiraModels;
import com.acme.herald.domain.dto.UpsertCase;
import com.acme.herald.provider.JiraProvider;
import com.acme.herald.web.JqlUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Service
@RequiredArgsConstructor
@Slf4j
public class LinkService {
    private final JiraProvider jira;
    private final AdminJiraConfigService jiraAdminCfg;

    TemplateLinkInfo resolveTemplateLinkInfo(UpsertCase req) {
        var cfg = jiraAdminCfg.getForRuntime();

        if (req.templateId() == null || req.templateId().isBlank()) {
            return new TemplateLinkInfo(null, null);
        }

        String linkTypeName = cfg.links() != null ? (String) cfg.links().templateToCase() : null;
        if (!isNotBlank(linkTypeName)) {
            return new TemplateLinkInfo(null, null);
        }

        String jql = "project = %s AND issuetype = \"%s\" AND %s ~ \"%s\""
                .formatted(
                        cfg.projectKey(),
                        JqlUtils.escapeJql(cfg.issueTypes().template()),
                        JqlUtils.toJqlField(cfg.fields().templateId()),
                        JqlUtils.escapeJql(req.templateId())
                );

        JiraModels.SearchResponse search = jira.search(jql, 0, 1);
        if (search.total() <= 0 || search.issues() == null || search.issues().isEmpty()) {
            return new TemplateLinkInfo(null, null);
        }

        Map<String, Object> template = search.issues().getFirst();
        String templateKey = (String) template.get("key");

        return new TemplateLinkInfo(templateKey, linkTypeName);
    }

    void ensureLinked(TemplateLinkInfo linkInfo, String caseKey) {
        if (!linkInfo.isLinkable() || !isNotBlank(caseKey)) return;

        if (isAlreadyLinked(caseKey, linkInfo.templateKey(), linkInfo.linkTypeName())) {
            return;
        }

        Map<String, Object> updateBody = Map.of(
                "update", updateRelatedLinkBody(linkInfo)
        );

        try {
            jira.updateIssue(caseKey, updateBody);
        } catch (RuntimeException e) {
            // fallback: niezależne od screenów
            safeCreateLinkFallback(linkInfo, caseKey);
        }
    }

    public Map<String, Object> updateRelatedLinkBody(TemplateLinkInfo linkInfo) {
        return Map.of(
                "issuelinks", List.of(Map.of(
                        "add", Map.of(
                                "type", Map.of("name", linkInfo.linkTypeName()),
                                "outwardIssue", Map.of("key", linkInfo.templateKey())
                        )
                ))
        );
    }

    public void safeCreateLinkFallback(TemplateLinkInfo linkInfo, String caseKey) {
        try {
            // link: Template (inward) <-> Case (outward) — kierunek nie jest krytyczny dla "Relates"
            jira.createIssueLink(linkInfo.linkTypeName(), linkInfo.templateKey(), caseKey);
        } catch (RuntimeException ex) {
            // MVP: nie blokujemy całego flow, ale logujemy (do diagnostyki)
            log.warn("Failed to create issue link (fallback). template={}, case={}, type={}. {}",
                    linkInfo.templateKey(), caseKey, linkInfo.linkTypeName(), safeMsg(ex));
        }
    }

    @SuppressWarnings("unchecked")
    private boolean isAlreadyLinked(String caseKey, String templateKey, String linkTypeName) {
        try {
            Map<String, Object> issue = jira.getIssue(caseKey, null);
            Map<String, Object> fields = (Map<String, Object>) issue.get("fields");
            if (fields == null) return false;

            List<Map<String, Object>> links = (List<Map<String, Object>>) fields.get("issuelinks");
            if (links == null) return false;

            for (Map<String, Object> l : links) {
                Map<String, Object> type = (Map<String, Object>) l.get("type");
                String name = type != null ? (String) type.get("name") : null;
                if (!linkTypeName.equals(name)) continue;

                Map<String, Object> inward = (Map<String, Object>) l.get("inwardIssue");
                Map<String, Object> outward = (Map<String, Object>) l.get("outwardIssue");

                String inKey = inward != null ? (String) inward.get("key") : null;
                String outKey = outward != null ? (String) outward.get("key") : null;

                if (templateKey.equals(inKey) || templateKey.equals(outKey)) return true;
            }

            return false;
        } catch (RuntimeException e) {
            return false;
        }
    }

    public static boolean looksLikeIssueLinksNotAllowed(RuntimeException e) {
        String m = String.valueOf(e.getMessage()).toLowerCase();
        // Jira różnie to opisuje zależnie od wersji/klienta. To jest “good enough” heurystyka:
        return m.contains("issuelinks")
                && (m.contains("cannot be set") || m.contains("field") || m.contains("unknown") || m.contains("not on the appropriate screen"));
    }

    public static String safeMsg(Throwable t) {
        String m = t.getMessage();
        if (m == null) return t.getClass().getSimpleName();
        return m.length() > 240 ? m.substring(0, 240) + "..." : m;
    }

    public record TemplateLinkInfo(String templateKey, String linkTypeName) {
        boolean isLinkable() {
            return isNotBlank(templateKey) && isNotBlank(linkTypeName);
        }
    }
}
