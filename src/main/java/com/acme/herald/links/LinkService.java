package com.acme.herald.links;

import com.acme.herald.config.JiraConfigService;
import com.acme.herald.domain.JiraModels;
import com.acme.herald.provider.JiraProvider;
import com.acme.herald.web.JqlUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Service
@RequiredArgsConstructor
@Slf4j
public class LinkService {
    private final JiraProvider jira;
    private final JiraConfigService jiraCfg;

    public List<JiraModels.IssueLinkType> list() {
        return jira.getIssueLinkTypes();
    }

    public String templateToCaseLinkTypeId() {
        var cfg = jiraCfg.getForRuntime();
        return (cfg.links() != null) ? cfg.links().templateToCase() : null;
    }

    public String templateToForkLinkTypeId() {
        var cfg = jiraCfg.getForRuntime();
        return (cfg.links() != null) ? cfg.links().templateToFork() : null;
    }

    public TemplateLinkInfo resolveTemplateLinkInfo(String templateId, String linkTypeId) {
        var cfg = jiraCfg.getForRuntime();
        String templateIdField = (cfg.fields() != null) ? cfg.fields().templateId() : null;

        if (isBlank(templateId) ||  isBlank(linkTypeId) || isBlank(templateIdField)) {
            return new TemplateLinkInfo(null, null);
        }

        String jql = "project = %s AND issuetype = \"%s\" AND %s ~ \"%s\""
                .formatted(
                        cfg.projectKey(),
                        JqlUtils.escapeJql(cfg.issueTypes().template()),
                        JqlUtils.toJqlField(templateIdField),
                        JqlUtils.escapeJql(templateId)
                );

        JiraModels.SearchResponse search = jira.search(jql, 0, 1);
        if (search == null || search.total() <= 0) {
            return new TemplateLinkInfo(null, null);
        }

        List<JsonNode> issues = search.issues();
        if (issues == null || issues.isEmpty()) {
            return new TemplateLinkInfo(null, null);
        }

        String templateKey = issues.getFirst().path("key").asString(null);
        if (!isNotBlank(templateKey)) {
            return new TemplateLinkInfo(null, null);
        }

        return new TemplateLinkInfo(templateKey, linkTypeId);
    }

    public void ensureLinked(TemplateLinkInfo linkInfo, String caseKey) {
        if (!linkInfo.isLinkable() || !isNotBlank(caseKey)) return;

        if (isAlreadyLinked(caseKey, linkInfo.templateKey(), linkInfo.linkTypeId())) {
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
                                "type", Map.of("id", linkInfo.linkTypeId()),
                                "outwardIssue", Map.of("key", linkInfo.templateKey())
                        )
                ))
        );
    }

    public void safeCreateLinkFallback(TemplateLinkInfo linkInfo, String caseKey) {
        try {
            // link: Template (inward) <-> Case (outward) — kierunek nie jest krytyczny dla "Relates"
            jira.createIssueLink(linkInfo.linkTypeId(), linkInfo.templateKey(), caseKey);
        } catch (RuntimeException ex) {
            // MVP: nie blokujemy całego flow, ale logujemy (do diagnostyki)
            log.warn("Failed to create issue link (fallback). template={}, case={}, type={}. {}",
                    linkInfo.templateKey(), caseKey, linkInfo.linkTypeId(), safeMsg(ex));
        }
    }

    private boolean isAlreadyLinked(String caseKey, String templateKey, String linkTypeId) {
        try {
            JsonNode issue = jira.getIssue(caseKey, null);

            JsonNode links = issue.path("fields").path("issuelinks");
            if (!links.isArray()) return false;

            for (JsonNode l : links) {
                String name = l.path("type").path("id").asString(null);
                if (!linkTypeId.equals(name)) continue;

                String inKey = l.path("inwardIssue").path("key").asString(null);
                String outKey = l.path("outwardIssue").path("key").asString(null);

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

    public record TemplateLinkInfo(String templateKey, String linkTypeId) {
        public boolean isLinkable() {
            return isNotBlank(templateKey) && isNotBlank(linkTypeId);
        }
    }
}
