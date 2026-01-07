package com.acme.herald.smartcase;

import com.acme.herald.config.JiraConfigService;
import com.acme.herald.config.JiraProperties;
import com.acme.herald.domain.JiraModels;
import com.acme.herald.domain.dto.CaseHistoryDtos;
import com.acme.herald.domain.dto.CaseRef;
import com.acme.herald.domain.dto.UpsertCase;
import com.acme.herald.provider.JiraProvider;
import com.acme.herald.web.JqlUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import java.util.*;

import static com.acme.herald.smartcase.LinkService.looksLikeIssueLinksNotAllowed;
import static com.acme.herald.smartcase.LinkService.safeMsg;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Slf4j
@Service
@RequiredArgsConstructor
public class CaseService {
    private final JiraProvider jira;
    private final JiraProperties jiraProps;
    private final JiraConfigService jiraCfg;
    private final LinkService linkService;

    public CaseRef upsertCase(UpsertCase req) {
        var cfg = jiraCfg.getForRuntime();
        var issueTypes = cfg.issueTypes();
        var fieldsCfg = cfg.fields();

        if (req.status() != null) {
            String cat = req.status().trim().toUpperCase();

            var map = cfg.status().caseStatusMap();
            if (map == null || map.isEmpty()) {
                throw new IllegalStateException("Brak konfiguracji statusów case (caseStatusMap).");
            }

            if (!map.containsKey(cat) || map.get(cat) == null || map.get(cat).isBlank()) {
                throw new IllegalArgumentException(
                        "Niepoprawna kategoria statusu case: " + req.status()
                                + ". Dozwolone: " + map.keySet()
                );
            }
        }

        // pola issue
        var fields = new HashMap<String, Object>();
        fields.put("summary", isNotBlank(req.summary()) ? req.summary() : req.caseId());
        fields.put("project", Map.of("key", jiraProps.getProjectKey()));
        fields.put("issuetype", Map.of("name", issueTypes.caseIssue()));
        fields.put("labels", req.labels() != null ? req.labels() : List.of());
        fields.put(fieldsCfg.caseId(), req.caseId());
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
                        JqlUtils.escapeJql(req.caseId()),
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
            JsonNode issue = existing.issues().stream().findAny().orElseThrow();
            caseKey = issue.get("key").asString(null);
            jira.updateIssue(caseKey, Map.of("fields", fields));
        }

        return new CaseRef(caseKey);
    }


    public CaseHistoryDtos.PayloadHistory payloadHistory(String issueKey) {
        var cfg = jiraCfg.getForRuntime();
        String fieldId = cfg.fields().payload();

        if (fieldId == null || fieldId.isBlank()) {
            throw new IllegalStateException("Brak konfiguracji cfg.fields.payload");
        }

        JsonNode issue = jira.getIssue(issueKey, null);
        String current = issue.path("fields").path(fieldId).asString(null);
        String updated = issue.path("fields").path("updated").asString(null);

        List<CaseHistoryDtos.PayloadVersion> changes = loadAllPayloadChanges(issueKey, fieldId);

        changes.sort(Comparator.comparing(CaseHistoryDtos.PayloadVersion::createdAt,
                Comparator.nullsLast(Comparator.naturalOrder())).reversed());

        List<CaseHistoryDtos.PayloadVersion> out = new ArrayList<>();
        out.add(new CaseHistoryDtos.PayloadVersion(
                "current",
                updated,
                "CURRENT",
                null,
                null,
                current,
                current != null ? current.length() : 0
        ));

        String last = current;
        for (var v : changes) {
            if (Objects.equals(last, v.toValue())) continue;
            last = v.toValue();
            out.add(v);
        }
        return new CaseHistoryDtos.PayloadHistory(issueKey, fieldId, List.copyOf(out));
    }

    private List<CaseHistoryDtos.PayloadVersion> loadAllPayloadChanges(String issueKey, String fieldId) {
        List<CaseHistoryDtos.PayloadVersion> out = new ArrayList<>();

        int startAt = 0;
        int max = 100;

        for (int guard = 0; guard < 200; guard++) {
            JiraModels.ChangelogPage page = jira.getIssueChangelog(issueKey, startAt, max);
            List<JiraModels.ChangelogHistory> histories =
                    page != null && page.histories() != null ? page.histories() : List.of();

            if (histories.isEmpty()) break;

            for (var h : histories) {
                String created = h.created();
                String hid = h.id();

                var au = h.author();
                String author = firstNonBlank(
                        au != null ? au.displayName() : null,
                        au != null ? au.name() : null,
                        au != null ? au.accountId() : null
                );
                String authorKey = firstNonBlank(
                        au != null ? au.key() : null,
                        au != null ? au.name() : null,
                        au != null ? au.accountId() : null
                );

                List<JiraModels.ChangelogHistoryItem> items =
                        h.items() != null ? h.items() : List.of();

                for (var it : items) {
                    if (it == null) continue;
                    String payloadFieldName = page.fieldNames().get(fieldId);
                    if (!payloadFieldName.equals(it.fieldName())) continue;

                    String fromV = it.fromText();
                    String toV = it.toText();

                    String id = "h:" + (hid != null ? hid : ("start:" + startAt)) + ":" + fieldId;

                    out.add(new CaseHistoryDtos.PayloadVersion(
                            id,
                            created,
                            author,
                            authorKey,
                            fromV,
                            toV,
                            toV != null ? toV.length() : 0
                    ));
                }
            }

            int total = page.total();
            startAt += histories.size();

            if (total > 0 && startAt >= total) break;
            if (histories.size() < max) break;
        }

        return out;
    }

    private static String firstNonBlank(String... vals) {
        if (vals == null) return null;
        for (String v : vals) {
            if (v != null && !v.isBlank()) return v;
        }
        return null;
    }

    public void like(String issueKey, boolean up) {
        jira.setVote(issueKey, up);
    }
}
