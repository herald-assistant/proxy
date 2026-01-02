// package com.acme.herald.provider;
package com.acme.herald.provider;

import com.acme.herald.domain.JiraModels;
import com.acme.herald.domain.JiraModels.IssueRef;
import com.acme.herald.domain.JiraModels.SearchResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface JiraProvider {
    String createPatByUsernamePd(String username, String pd, int days);

    JiraModels.UserResponse getMe();

    JiraModels.PermissionsResponse getMyPermissions(String projectKey, String issueKey, List<String> permissions);

    Map<String, Object> getProjectProperty(String projectKey, String propertyKey);

    void setProjectProperty(String projectKey, String propertyKey, Object propertyValue);

    IssueRef createIssue(Map<String, Object> body);

    Map<String, Object> getIssue(String issueKey, String expand);

    void updateIssue(String issueKey, Map<String, Object> body);

    // ───── NOWE KOMENTARZE ─────

    List<JiraModels.Comment> getComments(String issueKey);

    JiraModels.Comment addComment(String issueKey, String renderedBody);

    JiraModels.Comment updateComment(String issueKey, String commentId, String renderedBody);

    void deleteComment(String issueKey, String commentId);

    // ───── Reszta jak było ─────

    void setVote(String issueKey, boolean up);

    void addWatcher(String issueKey, String accountIdOrName);

    SearchResponse search(String jql, int startAt, int maxResults);

    void assignIssue(String key, JiraModels.AssigneePayload payload);

    List<JiraModels.AssignableUser> findAssignableUsers(String issueKey,
                                                        String projectKey,
                                                        String query,
                                                        int startAt,
                                                        int maxResults);

    JiraModels.Attachment attachAndReturnMeta(String issueKey, MultipartFile file);

    JiraModels.Attachment getAttachment(String attachmentId);

    byte[] downloadAttachment(String attachmentId);

    byte[] downloadAttachmentThumbnail(String attachmentId);

    Map<String, Object> getIssueProperty(String issueKey, String propertyKey);

    void setIssueProperty(String issueKey, String propertyKey, Object propertyValue);
}
