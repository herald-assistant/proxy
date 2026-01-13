package com.acme.herald.smarttemplate;

import com.acme.herald.config.JiraConfigService;
import com.acme.herald.config.JiraProperties;
import com.acme.herald.domain.JiraModels;
import com.acme.herald.domain.dto.TemplateRef;
import com.acme.herald.domain.dto.UpsertTemplate;
import com.acme.herald.links.LinkService;
import com.acme.herald.provider.JiraProvider;
import com.acme.herald.web.JqlUtils;
import com.acme.herald.web.dto.CommonDtos;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.acme.herald.links.LinkService.looksLikeIssueLinksNotAllowed;
import static com.acme.herald.links.LinkService.safeMsg;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Service
@RequiredArgsConstructor
@Slf4j
public class TemplateService {
    private final JiraProvider jira;
    private final JiraProperties jiraProps;
    private final JiraConfigService jiraCfg;
    private final LinkService linkService;

    public TemplateRef upsertTemplate(UpsertTemplate req) {
        var cfg = jiraCfg.getForRuntime();
        var issueTypes = cfg.issueTypes();
        var fieldsCfg = cfg.fields();

        if (req.status() != null) {
            String raw = req.status().trim();
            String cat = raw.toUpperCase();

            var map = cfg.status().templateStatusMap();
            if (map == null || map.isEmpty()) {
                throw new IllegalStateException("Brak konfiguracji statusów template (templateStatusMap).");
            }

            boolean okAsCategory = map.containsKey(cat) && map.get(cat) != null && !map.get(cat).isBlank();
            boolean okAsJiraStatusName = map.values().stream().anyMatch(v -> v != null && v.equalsIgnoreCase(raw));

            if (!okAsCategory && !okAsJiraStatusName) {
                throw new IllegalArgumentException(
                        "Niepoprawny status template: " + req.status()
                                + ". Dozwolone kategorie: " + map.keySet()
                                + " albo nazwy statusów Jira: " + map.values()
                );
            }
        }

        // pola issue
        var fields = new java.util.HashMap<String, Object>();
        fields.put("summary", isNotBlank(req.title()) ? req.title() : req.templateId());
        fields.put("project", Map.of("key", jiraProps.getProjectKey()));
        fields.put("issuetype", Map.of("name", issueTypes.template()));
        fields.put("labels", req.labels() != null ? req.labels() : List.of());
        fields.put(fieldsCfg.templateId(), req.templateId());
        fields.put(fieldsCfg.payload(), req.payload().toString());
        fields.put(fieldsCfg.templateStatus(), req.status());

        String jql = "%s ~ \"%s\""
                .formatted(
                        JqlUtils.toJqlField(fieldsCfg.templateId()),
                        JqlUtils.escapeJql(req.templateId())
                );

        JiraModels.SearchResponse existing = jira.search(jql, 0, 1);

        String templateKey;
        if (existing.total() <= 0 || existing.issues() == null || existing.issues().isEmpty()) {
            // CREATE
            Map<String, Object> createBody = new HashMap<>();
            createBody.put("fields", fields);

            LinkService.TemplateLinkInfo linkInfo = linkService.resolveTemplateLinkInfo(req.sourceTemplateId(), linkService.templateToForkLinkTypeId());
            if (linkInfo.isLinkable()) {
                createBody.put("update", linkService.updateRelatedLinkBody(linkInfo));
            }

            try {
                templateKey = jira.createIssue(createBody).key();
            } catch (RuntimeException e) {
                if (linkInfo.isLinkable() && looksLikeIssueLinksNotAllowed(e)) {
                    log.warn("Create with update.issuelinks failed; falling back to create+issueLink. {}", safeMsg(e));
                    templateKey = jira.createIssue(Map.of("fields", fields)).key();
                    linkService.safeCreateLinkFallback(linkInfo, templateKey);
                } else {
                    throw e;
                }
            }

            if (linkInfo.isLinkable()) {
                linkService.ensureLinked(linkInfo, templateKey);
            }
        } else {
            // UPDATE
            var issue = existing.issues().getFirst();
            templateKey = issue.path("key").asString();
            if (!isNotBlank(templateKey)) {
                throw new IllegalStateException("Jira search returned issue without key for jql: " + jql);
            }
            jira.updateIssue(templateKey, Map.of("fields", fields));
        }

        String url = jiraProps.getBaseUrl() + "/browse/" + templateKey;
        return new TemplateRef(templateKey, url);
    }

    public void like(String issueKey, CommonDtos.LikeReq req) {
        jira.setVote(issueKey, req.liked());
    }
}
