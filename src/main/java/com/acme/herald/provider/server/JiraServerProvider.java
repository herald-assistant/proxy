package com.acme.herald.provider.server;

import com.acme.herald.auth.TokenPayload;
import com.acme.herald.config.JiraProperties;
import com.acme.herald.domain.JiraModels;
import com.acme.herald.domain.JiraModels.IssueRef;
import com.acme.herald.domain.JiraModels.SearchResponse;
import com.acme.herald.provider.JiraProvider;
import com.acme.herald.provider.feign.JiraApiV2Client;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class JiraServerProvider implements JiraProvider {
    private final JiraApiV2Client api;
    private final HttpServletRequest req;
    private final RestClient rest = RestClient.builder().build();

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
    public Map<String, Object> getProjectProperty(String projectKey, String propertyKey) {
        var tp = currentAuth();
        try {
            return api.getProjectProperty(auth(tp), projectKey, propertyKey);
        } catch (RuntimeException e) {
            log.error("Exception during fetching ProjectProperty: " + projectKey + ", returning empty map" + e.getMessage());
            // Jira: property nie istnieje => 404. Traktujemy jako "brak", a nie błąd.
            return Map.of();
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
    public Map<String, Object> getIssue(String issueKey, String expand) {
        var tp = currentAuth();
        return api.getIssue(auth(tp), issueKey, expand);
    }

    @Override
    public void updateIssue(String issueKey, Map<String, Object> body) {
        var tp = (TokenPayload) req.getAttribute("herald.currentAuth");
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
        var m = api.search(auth(tp), jql, startAt, maxResults);
        var issues = (List<Map<String, Object>>) m.get("issues");
        return new SearchResponse((int) m.get("startAt"), (int) m.get("maxResults"), (int) m.get("total"), issues);
    }

    @Override
    public void assignIssue(String key, JiraModels.AssigneePayload payload) {
        var tp = currentAuth();
        api.assignIssue(auth(tp), key, payload);
    }

    @Override
    public List<JiraModels.AssignableUser> findAssignableUsers(String issueKey, String projectKey, String username, int startAt, int maxResults) {
        var tp = currentAuth();
        return api.findAssignableUsers(auth(tp), issueKey, projectKey, username, startAt, maxResults);
    }

    @Override
    public JiraModels.Attachment attachAndReturnMeta(String issueKey, MultipartFile file) {
        var tp = currentAuth();
        // Jira Server/DC zwykle wymaga X-Atlassian-Token: no-check
        List<JiraModels.Attachment> list = api.attachAndReturnMeta(auth(tp), "no-check", issueKey, file);
        // Jira zwraca listę załączników dodanych – nas interesuje pierwszy
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
    public Map<String, Object> getIssueProperty(String issueKey, String propertyKey) {
        var tp = currentAuth();
        try {
            return api.getIssueProperty(auth(tp), issueKey, propertyKey);
        } catch (RuntimeException e) {
            log.warn("Exception during fetching IssueProperty: " + propertyKey + ", returning empty map" + e.getMessage());
            return Map.of();
        }
    }

    @Override
    public void setIssueProperty(String issueKey, String propertyKey, Object propertyValue) {
        var tp = currentAuth();
        api.setIssueProperty(auth(tp), issueKey, propertyKey, propertyValue);
    }

    // ───── KOMENTARZE ─────

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

    // ─────────────────────────────────────────

    private TokenPayload currentAuth() {
        Object o = req.getAttribute("herald.currentAuth");
        if (!(o instanceof TokenPayload tp) || tp.token() == null || tp.token().isBlank()) {
            throw new IllegalStateException("Brak herald.currentAuth w request (TokenPayload).");
        }
        return tp;
    }


    private String auth(TokenPayload tp) {
        return "Bearer " + tp.token();
    }
}
