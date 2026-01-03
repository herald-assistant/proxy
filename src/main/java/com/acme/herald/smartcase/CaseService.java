package com.acme.herald.smartcase;

import com.acme.herald.config.AdminJiraConfigService;
import com.acme.herald.config.JiraProperties;
import com.acme.herald.domain.dto.CaseRef;
import com.acme.herald.domain.dto.CreateCase;
import com.acme.herald.domain.dto.RatingInput;
import com.acme.herald.domain.dto.RatingResult;
import com.acme.herald.provider.JiraProvider;
import com.acme.herald.web.JqlUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.acme.herald.smartcase.LinkService.looksLikeIssueLinksNotAllowed;
import static com.acme.herald.smartcase.LinkService.safeMsg;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Slf4j
@Service
@RequiredArgsConstructor
public class CaseService {
    private final JiraProvider jira;
    private final JiraProperties jiraProps;
    private final AdminJiraConfigService jiraAdminCfg;
    private final LinkService linkService;

    public CaseRef upsertCase(CreateCase req) {
        var cfg = jiraAdminCfg.getForRuntime();
        var issueTypes = cfg.issueTypes();
        var fieldsCfg = cfg.fields();

        if (req.status() != null && cfg.status().caseFlow().stream().noneMatch(s -> s.equals(req.status()))) {
            throw new IllegalArgumentException("Niepoprawny status case: " + req.status() + ". Dozwolone: " + cfg.status().caseFlow());
        }

        // pola issue
        var fields = new HashMap<String, Object>();
        fields.put("summary", isNotBlank(req.summary()) ? req.summary() : req.case_id());
        fields.put("project", Map.of("key", jiraProps.getProjectKey()));
        fields.put("issuetype", Map.of("name", issueTypes.caseIssue()));
        fields.put("labels", req.labels() != null ? req.labels() : List.of());
        fields.put(fieldsCfg.caseId(), req.case_id());
        fields.put(fieldsCfg.payload(), req.payload().toString());

//        fields.put(fieldsCfg.templateId(), req.template_id());
//        fields.put("summary", ofNullable(req.summary()).orElse(""));
//        fields.put("description", ofNullable(req.description()).orElse(""));
//        fields.put(fieldsCfg.casePayload(), req.description());

        if (fieldsCfg.caseStatus() != null && !fieldsCfg.caseStatus().isBlank()) {
            fields.put(fieldsCfg.caseStatus(), req.status());
        }

        String jql = "project = %s AND %s ~ \"%s\" AND issuetype = \"%s\""
                .formatted(
                        cfg.projectKey(),
                        JqlUtils.toJqlField(fieldsCfg.caseId()),
                        JqlUtils.escapeJql(req.case_id()),
                        JqlUtils.escapeJql(cfg.issueTypes().caseIssue())
                );

        var existing = jira.search(jql, 0, 1);

        String caseKey;

        if (existing.total() == 0) {
            // CREATE
            Map<String, Object> createBody = new HashMap<>();
            createBody.put("fields", fields);


            LinkService.TemplateLinkInfo linkInfo = linkService.resolveTemplateLinkInfo(req);
            if (linkInfo.isLinkable()) {
                createBody.put("update", linkService.updateRelatedLinkBody(linkInfo));
            }

            try {
                caseKey = jira.createIssue(createBody).key();
            } catch (RuntimeException e) {
                if (linkInfo.isLinkable() && looksLikeIssueLinksNotAllowed(e)) {
                    log.warn("Create with update.issuelinks failed; falling back to create+issueLink. {}", safeMsg(e));
                    caseKey = jira.createIssue(Map.of("fields", fields)).key();
                    linkService.safeCreateLinkFallback(linkInfo, caseKey);
                } else {
                    throw e;
                }
            }

            if (linkInfo.isLinkable()) {
                linkService.ensureLinked(linkInfo, caseKey);
            }

        } else {
            // UPDATE
            Map<String, Object> issue = existing.issues().stream().findAny().orElseThrow();
            caseKey = String.valueOf(issue.get("key"));
            jira.updateIssue(caseKey, Map.of("fields", fields));
        }

        return new CaseRef(caseKey);
    }

    public void commentWithMentions(String caseKey, String text) {
        jira.addComment(caseKey, text);
    }

    public void like(String issueKey, boolean up) {
        jira.setVote(issueKey, up);
    }

    public RatingResult rate(String caseKey, RatingInput rating) {
        var cfg = jiraAdminCfg.getForRuntime();
        var ratingField = cfg.fields().ratingAvg();

        double avg = rating.categories().stream().mapToDouble(RatingInput.Category::value).average().orElse(0.0);
        Map<String, Object> body = Map.of("fields", Map.of(ratingField, avg));
        jira.updateIssue(caseKey, body);
        return new RatingResult(avg);
    }
}
