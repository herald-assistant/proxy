package com.acme.herald.provider;

import com.acme.herald.assignee.dto.AssigneeDtos;
import com.acme.herald.auth.TokenPayload;
import com.acme.herald.domain.JiraModels;
import com.acme.herald.domain.JiraModels.IssueRef;
import com.acme.herald.domain.JiraModels.SearchResponse;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

public interface JiraProvider {
    TokenPayload createPatByUsernamePdWithMeta(String username, String pd, int days);
    void revokeCurrentPat();

    JiraModels.UserResponse getMe();
    JiraModels.PermissionsResponse getMyPermissions(String projectKey, String issueKey, List<String> permissions);
    List<String> groupPicker(String query, List<String> exclude, int maxResults);

    JsonNode getProjectProperty(String projectKey, String propertyKey);
    void setProjectProperty(String projectKey, String propertyKey, Object propertyValue);

    IssueRef createIssue(Map<String, Object> body); // może zostać jak jest (Map jest OK jako request)
    JsonNode getIssue(String issueKey, String expand);
    void updateIssue(String issueKey, Map<String, Object> body);

    void setVote(String issueKey, boolean up);
    void addWatcher(String issueKey, String accountIdOrName);

    SearchResponse search(String jql, int startAt, int maxResults);

    void assignIssue(String key, AssigneeDtos.AssigneeReq payload);
    List<JiraModels.AssignableUser> findAssignableUsers(String issueKey, String projectKey, String query, int startAt, int maxResults);

    JiraModels.Attachment attachAndReturnMeta(String issueKey, MultipartFile file);
    JiraModels.Attachment getAttachment(String attachmentId);
    byte[] downloadAttachment(String attachmentId);
    byte[] downloadAttachmentThumbnail(String attachmentId);

    JsonNode getIssueProperty(String issueKey, String propertyKey);
    void setIssueProperty(String issueKey, String propertyKey, Object propertyValue);

    void createIssueLink(String linkTypeName, String issueKey, String caseKey);

    // comments
    List<JiraModels.Comment> getComments(String issueKey);
    JiraModels.Comment addComment(String issueKey, String renderedBody);
    JiraModels.Comment updateComment(String issueKey, String commentId, String renderedBody);
    void deleteComment(String issueKey, String commentId);
}
