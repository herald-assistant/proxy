package com.acme.herald.provider.server;

import com.acme.herald.auth.TokenPayload;
import com.acme.herald.config.JiraProperties;
import com.acme.herald.domain.JiraModels;
import com.acme.herald.domain.JiraModels.IssueRef;
import com.acme.herald.domain.JiraModels.SearchResponse;
import com.acme.herald.provider.JiraProvider;
import com.acme.herald.provider.feign.JiraApiV2Client;
import com.acme.herald.web.error.ConflictException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class JiraServerProvider implements JiraProvider {
    private final JiraApiV2Client api;
    private final JiraProperties props;
    private final HttpServletRequest req;

    @Override
    public JiraModels.UserResponse getMe() {
        var tp = (TokenPayload) req.getAttribute("herald.currentAuth");
        return api.getMe(auth(tp));
    }

    @Override
    public IssueRef createIssue(Map<String, Object> body) {
        var tp = (TokenPayload) req.getAttribute("herald.currentAuth");
        var resp = api.createIssue(auth(tp), body);
        return new IssueRef(resp.id(), resp.key(), 1);
    }

    @Override
    public Map<String, Object> getIssue(String issueKey, String expand) {
        var tp = (TokenPayload) req.getAttribute("herald.currentAuth");
        return api.getIssue(auth(tp), issueKey, expand);
    }

    @Override
    public void updateIssue(String issueKey, Map<String, Object> body, Integer expectedVersion) {
        var tp = (TokenPayload) req.getAttribute("herald.currentAuth");
        // optimistic lock (Server): porównujemy pola version z GET — jeśli nie pasuje, Conflict
        if (props.getOptions().getOrDefault("useOptimisticLock", true) && expectedVersion != null) {
            var issue = api.getIssue(auth(tp), issueKey, "versions,changelog");
            Integer current = (Integer) ((Map<String, Object>) issue.get("fields")).get("version");
            if (current != null && !current.equals(expectedVersion)) {
                throw new ConflictException("Version mismatch: expected " + expectedVersion + " got " + current);
            }
        }
        api.updateIssue(auth(tp), issueKey, body);
    }

    @Override
    public void transition(String issueKey, String transitionId) {
        var tp = (TokenPayload) req.getAttribute("herald.currentAuth");
        api.transition(auth(tp), issueKey, Map.of("transition", Map.of("id", transitionId)));
    }

    @Override
    public JiraModels.TransitionList transitions(String issueKey) {
        var tp = (TokenPayload) req.getAttribute("herald.currentAuth");
        return api.transitions(auth(tp), issueKey);
    }

    @Override
    public void setVote(String issueKey, boolean up) {
        var tp = (TokenPayload) req.getAttribute("herald.currentAuth");
        if (up) api.addVote(auth(tp), issueKey);
        else api.removeVote(auth(tp), issueKey);
    }

    @Override
    public void addWatcher(String issueKey, String accountIdOrName) {
        var tp = (TokenPayload) req.getAttribute("herald.currentAuth");
        api.addWatcher(auth(tp), issueKey, accountIdOrName);
    }

    @Override
    public SearchResponse search(String jql, int startAt, int maxResults) {
        var tp = (TokenPayload) req.getAttribute("herald.currentAuth");
        var m = api.search(auth(tp), jql, startAt, maxResults);
        var issues = (List<Map<String, Object>>) m.get("issues");
        return new SearchResponse((int) m.get("startAt"), (int) m.get("maxResults"), (int) m.get("total"), issues);
    }

    @Override
    public void assignIssue(String key, JiraModels.AssigneePayload payload) {
        var tp = (TokenPayload) req.getAttribute("herald.currentAuth");
        api.assignIssue(auth(tp), key, payload);
    }

    @Override
    public List<JiraModels.AssignableUser> findAssignableUsers(String issueKey, String projectKey, String username, int startAt, int maxResults) {
        var tp = (TokenPayload) req.getAttribute("herald.currentAuth");
        return api.findAssignableUsers(auth(tp), issueKey, projectKey, username, startAt, maxResults);
    }

    @Override
    public JiraModels.Attachment attachAndReturnMeta(String issueKey, MultipartFile file) {
        var tp = (TokenPayload) req.getAttribute("herald.currentAuth");
        List<JiraModels.Attachment> list = api.attachAndReturnMeta(auth(tp), "no-check", issueKey, file);
        // Jira zwraca listę załączników dodanych – nas interesuje pierwszy
        return list.getFirst();
    }

    @Override
    public JiraModels.Attachment getAttachment(String attachmentId) {
        var tp = (TokenPayload) req.getAttribute("herald.currentAuth");
        return api.getAttachment(auth(tp), attachmentId);
    }

    @Override
    public byte[] downloadAttachment(String attachmentId) {
        var tp = (TokenPayload) req.getAttribute("herald.currentAuth");
        var meta = api.getAttachment(auth(tp), attachmentId);

        return rest().get()
                .uri(meta.content())
                .header(HttpHeaders.AUTHORIZATION, auth(tp))
                .retrieve()
                .body(byte[].class);
    }

    @Override
    public byte[] downloadAttachmentThumbnail(String attachmentId) {
        var tp = (TokenPayload) req.getAttribute("herald.currentAuth");
        var meta = api.getAttachment(auth(tp), attachmentId);
        var thumbUrl = meta.thumbnail() != null ? meta.thumbnail() : meta.content();

        return rest().get()
                .uri(thumbUrl)
                .header(HttpHeaders.AUTHORIZATION, auth(tp))
                .retrieve()
                .body(byte[].class);
    }

    @Override
    public Map<String, Object> getIssueProperty(String issueKey, String propertyKey) {
        var tp = (TokenPayload) req.getAttribute("herald.currentAuth");
        return api.getIssueProperty(auth(tp), issueKey, propertyKey);
    }

    @Override
    public void setIssueProperty(String issueKey, String propertyKey, Object propertyValue) {
        var tp = (TokenPayload) req.getAttribute("herald.currentAuth");
        api.setIssueProperty(auth(tp), issueKey, propertyKey, propertyValue);
    }

    // ───── KOMENTARZE ─────

    @Override
    public List<JiraModels.Comment> getComments(String issueKey) {
        var tp = (TokenPayload) req.getAttribute("herald.currentAuth");
        JiraModels.CommentPage page = api.getComments(auth(tp), issueKey);
        return page.comments();
    }

    @Override
    public JiraModels.Comment addComment(String issueKey, String renderedBody) {
        var tp = (TokenPayload) req.getAttribute("herald.currentAuth");
        return api.addComment(auth(tp), issueKey, Map.of("body", renderedBody));
    }

    @Override
    public JiraModels.Comment updateComment(String issueKey, String commentId, String renderedBody) {
        var tp = (TokenPayload) req.getAttribute("herald.currentAuth");
        return api.updateComment(auth(tp), issueKey, commentId, Map.of("body", renderedBody));
    }

    @Override
    public void deleteComment(String issueKey, String commentId) {
        var tp = (TokenPayload) req.getAttribute("herald.currentAuth");
        api.deleteComment(auth(tp), issueKey, commentId);
    }

    private RestClient rest() {
        // RestClient Spring 6 (następca RestTemplate) – prosty klient do proxy GET
        return RestClient.builder().build();
    }


    private String auth(TokenPayload tp) {
        return "Bearer " + tp.token();
    }
}
