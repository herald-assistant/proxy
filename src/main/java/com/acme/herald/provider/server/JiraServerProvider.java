package com.acme.herald.provider.server;

import com.acme.herald.assignee.dto.AssigneeDtos;
import com.acme.herald.auth.JiraAuthorization;
import com.acme.herald.auth.StatelessAuthFilter;
import com.acme.herald.auth.TokenPayload;
import com.acme.herald.domain.JiraModels;
import com.acme.herald.domain.JiraModels.IssueRef;
import com.acme.herald.domain.JiraModels.SearchResponse;
import com.acme.herald.provider.JiraProvider;
import com.acme.herald.provider.feign.JiraApiV2Client;
import feign.FeignException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.JsonNodeFactory;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class JiraServerProvider implements JiraProvider {
    private static final JsonNodeFactory NF = JsonNodeFactory.instance;
    private final JiraApiV2Client api;
    private final HttpServletRequest req;
    private final RestClient rest = RestClient.builder().build();
    private final JsonMapper jsonMapper;

    @Override
    public TokenPayload createPatByUsernamePdWithMeta(String username, String pd, int days) {
        String basic = toBasicAuth(username, pd);

        var tokenName = "Herald Auto Token (" + days + "d)";
        JiraModels.JiraPatCreateResponse res =
                api.createPatToken(basic, new JiraModels.JiraPatCreateRequest(tokenName, days));

        if (res == null || res.rawToken() == null || res.rawToken().isBlank()) {
            throw new IllegalStateException("JIRA_PAT_NO_RAW_TOKEN");
        }
        if (res.id() == null) {
            throw new IllegalStateException("JIRA_PAT_NO_ID");
        }

        return new TokenPayload(res.rawToken(), Instant.now().plus(Duration.ofDays(days)), res.id());
    }

    @Override
    public void revokeCurrentPat() {
        var tp = currentAuth();
        if (tp.patId() == null) return;
        api.revokePatToken(auth(tp), tp.patId());
    }

    private static String toBasicAuth(String username, String pd) {
        String raw = username + ":" + pd;
        String b64 = Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
        return "Basic " + b64;
    }

    @Override
    public JiraModels.UserResponse getMe() {
        var tp = currentAuth();
        return api.getMe(auth(tp), "groups");
    }

    @Override
    public JiraModels.PermissionsResponse getMyPermissions(String projectKey, String issueKey, List<String> permissions) {
        var tp = currentAuth();

        String permissionsCsv = (permissions == null || permissions.isEmpty())
                ? null
                : String.join(",", permissions);

        return api.myPermissions(auth(tp), projectKey, issueKey, permissionsCsv);
    }

    @Override
    public List<String> groupPicker(String query, List<String> exclude, int maxResults) {
        var tp = currentAuth();

        String ex = (exclude == null || exclude.isEmpty()) ? null : String.join(",", exclude);
        int lim = Math.max(1, Math.min(maxResults, 1000));

        JsonNode raw = api.groupPicker(auth(tp), query, ex, lim);

        JsonNode groups = raw.path("groups");

        List<String> out = new ArrayList<>();
        if (groups != null && groups.isArray()) {
            for (JsonNode g : groups) {
                String name = g.path("name").asString("");
                if (!name.isBlank()) out.add(name);
            }
        }
        return List.copyOf(out);
    }

    @Override
    public JsonNode getIssueProperty(String issueKey, String propertyKey) {
        var tp = currentAuth();
        try {
            JsonNode raw = api.getIssueProperty(auth(tp), issueKey, propertyKey);
            return unwrapPropertyValue(raw);
        } catch (FeignException.NotFound e) {
            return emptyObj();
        } catch (RuntimeException e) {
            log.warn("Exception during fetching IssueProperty: {}, returning empty object. {}",
                    propertyKey, e.getMessage());
            return emptyObj();
        }
    }

    @Override
    public JsonNode getProjectProperty(String projectKey, String propertyKey) {
        var tp = currentAuth();
        try {
            JsonNode raw = api.getProjectProperty(auth(tp), projectKey, propertyKey);
            return unwrapPropertyValue(raw);
        } catch (FeignException.NotFound e) {
            return emptyObj();
        } catch (RuntimeException e) {
            log.warn("Exception during fetching ProjectProperty: {}, returning empty object. {}",
                    propertyKey, e.getMessage());
            return emptyObj();
        }
    }

    @Override
    public void setProjectProperty(String projectKey, String propertyKey, Object propertyValue) {
        var tp = currentAuth();
        api.setProjectProperty(auth(tp), projectKey, propertyKey, propertyValue);
    }

    @Override
    public IssueRef createIssue(Map<String, Object> body) {
        var tp = currentAuth();
        var resp = api.createIssue(auth(tp), body);
        return new IssueRef(resp.id(), resp.key(), 1);
    }

    @Override
    public JsonNode getIssue(String issueKey, String expand) {
        var tp = currentAuth();
        try {
            JsonNode raw = api.getIssue(auth(tp), issueKey, expand);
            return unwrapPropertyValue(raw);
        } catch (FeignException.NotFound e) {
            return emptyObj();
        } catch (RuntimeException e) {
            log.warn("Exception during fetching Issue: {}, returning empty object. {}",
                    issueKey, e.getMessage());
            return emptyObj();
        }
    }


    @Override
    public void updateIssue(String issueKey, Map<String, Object> body) {
        var tp = currentAuth();
        api.updateIssue(auth(tp), issueKey, body);
    }

    @Override
    public void setVote(String issueKey, boolean up) {
        var tp = currentAuth();
        if (up) api.addVote(auth(tp), issueKey);
        else api.removeVote(auth(tp), issueKey);
    }

    @Override
    public void addWatcher(String issueKey, String accountIdOrName) {
        var tp = currentAuth();
        api.addWatcher(auth(tp), issueKey, accountIdOrName);
    }

    @Override
    public SearchResponse search(String jql, int startAt, int maxResults) {
        var tp = currentAuth();

        JsonNode m = api.search(auth(tp), jql, startAt, maxResults);

        int start = m.path("startAt").asInt(0);
        int max = m.path("maxResults").asInt(0);
        int total = m.path("total").asInt(0);

        JsonNode issuesNode = m.path("issues");
        List<JsonNode> issues = new ArrayList<>();
        if (issuesNode != null && issuesNode.isArray()) {
            for (JsonNode issue : issuesNode) {
                issues.add(issue);
            }
        }

        return new SearchResponse(start, max, total, List.copyOf(issues));
    }

    @Override
    public void assignIssue(String key, AssigneeDtos.AssigneeReq payload) {
        var tp = currentAuth();
        api.assignIssue(auth(tp), key, new JiraModels.AssigneePayload(payload.name(), payload.key()));
    }

    @Override
    public List<JiraModels.AssignableUser> findAssignableUsers(String issueKey, String projectKey, String username, int startAt, int maxResults) {
        var tp = currentAuth();
        return api.findAssignableUsers(auth(tp), issueKey, projectKey, username, startAt, maxResults);
    }

    @Override
    public JiraModels.Attachment attachAndReturnMeta(String issueKey, MultipartFile file) {
        var tp = currentAuth();
        List<JiraModels.Attachment> list = api.attachAndReturnMeta(auth(tp), "no-check", issueKey, file);
        return list.getFirst();
    }

    @Override
    public JiraModels.Attachment getAttachment(String attachmentId) {
        var tp = currentAuth();
        return api.getAttachment(auth(tp), attachmentId);
    }

    @Override
    public byte[] downloadAttachment(String attachmentId) {
        var tp = currentAuth();
        var meta = api.getAttachment(auth(tp), attachmentId);

        return rest.get()
                .uri(meta.content())
                .header(HttpHeaders.AUTHORIZATION, auth(tp))
                .retrieve()
                .body(byte[].class);
    }

    @Override
    public byte[] downloadAttachmentThumbnail(String attachmentId) {
        var tp = currentAuth();
        var meta = api.getAttachment(auth(tp), attachmentId);
        var thumbUrl = meta.thumbnail() != null ? meta.thumbnail() : meta.content();

        return rest.get()
                .uri(thumbUrl)
                .header(HttpHeaders.AUTHORIZATION, auth(tp))
                .retrieve()
                .body(byte[].class);
    }

    @Override
    public void setIssueProperty(String issueKey, String propertyKey, Object propertyValue) {
        var tp = currentAuth();
        api.setIssueProperty(auth(tp), issueKey, propertyKey, propertyValue);
    }

    @Override
    public void createIssueLink(String linkTypeName, String inwardIssueKey, String outwardIssueKey) {
        var tp = currentAuth();

        Map<String, Object> body = Map.of(
                "type", Map.of("name", linkTypeName),
                "inwardIssue", Map.of("key", inwardIssueKey),
                "outwardIssue", Map.of("key", outwardIssueKey)
        );

        api.createIssueLink(auth(tp), body);
    }

    @Override
    public List<JiraModels.Comment> getComments(String issueKey) {
        var tp = currentAuth();
        JiraModels.CommentPage page = api.getComments(auth(tp), issueKey);
        return page.comments();
    }

    @Override
    public JiraModels.Comment addComment(String issueKey, String renderedBody) {
        var tp = currentAuth();
        return api.addComment(auth(tp), issueKey, Map.of("body", renderedBody));
    }

    @Override
    public JiraModels.Comment updateComment(String issueKey, String commentId, String renderedBody) {
        var tp = currentAuth();
        return api.updateComment(auth(tp), issueKey, commentId, Map.of("body", renderedBody));
    }

    @Override
    public void deleteComment(String issueKey, String commentId) {
        var tp = currentAuth();
        api.deleteComment(auth(tp), issueKey, commentId);
    }

    @Override
    public JiraModels.ChangelogPage getIssueChangelog(String issueKey, int startAt, int maxResults) {
        var tp = currentAuth();
        try {
            JsonNode issue = api.getIssue(auth(tp), issueKey, "changelog,names");
            JsonNode changelog = issue.path("changelog");
            JsonNode names = issue.path("names");
            return toChangelogPage(changelog, names, startAt, maxResults);
        } catch (RuntimeException e) {
            log.warn("Exception during fetching Changelog: {}, returning empty page. {}",
                    issueKey, safeMsg(e));
            return emptyChangelogPage(startAt, maxResults);
        }
    }

    @Override
    public List<JiraModels.IssueLinkType> getIssueLinkTypes() {
        var tp = currentAuth();
        return api.getIssueLinkTypes(auth(tp)).issueLinkTypes();
    }

    private JiraModels.ChangelogPage toChangelogPage(
            JsonNode changelog,
            JsonNode names,
            int fallbackStartAt,
            int fallbackMaxResults
    ) {
        try {
            if (changelog == null || changelog.isNull() || changelog.isMissingNode()) {
                return emptyChangelogPage(fallbackStartAt, fallbackMaxResults);
            }

            // 1) changelog -> typowany core (bez fieldNames)
            JiraModels.ChangelogPageCore core = jsonMapper.treeToValue(changelog, JiraModels.ChangelogPageCore.class);
            if (core == null) {
                return emptyChangelogPage(fallbackStartAt, fallbackMaxResults);
            }

            // 2) names -> Map<String,String> (fieldId -> label)
            Map<String, String> fieldNames = jsonMapper.convertValue(names, Map.class);

            // 3) złożenie finalnego modelu
            return new JiraModels.ChangelogPage(
                    core.startAt(),
                    core.maxResults(),
                    core.total(),
                    fieldNames,
                    core.histories() != null ? core.histories() : List.of()
            );

        } catch (Exception e) {
            log.warn("Failed to map changelog/names to ChangelogPage, returning empty. {}", safeMsg(e));
            return emptyChangelogPage(fallbackStartAt, fallbackMaxResults);
        }
    }

    private static JiraModels.ChangelogPage emptyChangelogPage(int startAt, int maxResults) {
        return new JiraModels.ChangelogPage(startAt, maxResults, 0, Map.of(), List.of());
    }

    private TokenPayload currentAuth() {
        Object o = req.getAttribute(StatelessAuthFilter.ATTR_CURRENT_AUTH);
        if (!(o instanceof TokenPayload tp) || tp.token() == null || tp.token().isBlank()) {
            throw new IllegalStateException("Missing herald.currentAuth in request (TokenPayload).");
        }
        return tp;
    }

    private static JsonNode unwrapPropertyValue(JsonNode raw) {
        if (raw == null || raw.isNull() || raw.isMissingNode()) return emptyObj();
        JsonNode v = raw.get("value");
        if (v != null && !v.isNull() && !v.isMissingNode()) return v;
        // fallback: czasem możesz dostać już samą wartość
        return raw;
    }

    private static JsonNode emptyObj() {
        return NF.objectNode();
    }

    private String auth(TokenPayload tp) {
        return JiraAuthorization.auth(tp);
    }

    private static String safeMsg(Throwable t) {
        String m = t.getMessage();
        if (m == null) return t.getClass().getSimpleName();
        return m.length() > 400 ? m.substring(0, 400) + "…" : m;
    }
}
