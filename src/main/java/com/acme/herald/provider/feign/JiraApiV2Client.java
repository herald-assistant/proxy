package com.acme.herald.provider.feign;

import com.acme.herald.domain.JiraModels;
import com.acme.herald.domain.JiraModels.CreateIssueResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@FeignClient(name = "jiraV2", url = "${jira.baseUrl}/rest/api/2")
public interface JiraApiV2Client {

    @GetMapping(value = "/myself", consumes = MediaType.APPLICATION_JSON_VALUE)
    JiraModels.UserResponse getMe(@RequestHeader("Authorization") String auth);

    @GetMapping(value = "/mypermissions", consumes = MediaType.APPLICATION_JSON_VALUE)
    JiraModels.PermissionsResponse myPermissions(
            @RequestHeader("Authorization") String auth,
            @RequestParam(required = false) String projectKey,
            @RequestParam(required = false) String issueKey,
            @RequestParam(required = false) String permissions
    );

    @GetMapping("/project/{projectKey}/properties/{propertyKey}")
    Map<String, Object> getProjectProperty(
            @RequestHeader("Authorization") String auth,
            @PathVariable("projectKey") String projectKey,
            @PathVariable("propertyKey") String propertyKey
    );

    @PutMapping(value = "/project/{projectKey}/properties/{propertyKey}", consumes = MediaType.APPLICATION_JSON_VALUE)
    void setProjectProperty(
            @RequestHeader("Authorization") String auth,
            @PathVariable("projectKey") String projectKey,
            @PathVariable("propertyKey") String propertyKey,
            @RequestBody Object propertyValue
    );

    @PostMapping(value = "/issue", consumes = MediaType.APPLICATION_JSON_VALUE)
    CreateIssueResponse createIssue(@RequestHeader("Authorization") String auth, @RequestBody Map<String, Object> body);

    @GetMapping("/issue/{key}")
    Map<String, Object> getIssue(@RequestHeader("Authorization") String auth, @PathVariable String key, @RequestParam(required = false) String expand);

    @PutMapping(value = "/issue/{key}", consumes = MediaType.APPLICATION_JSON_VALUE)
    void updateIssue(@RequestHeader("Authorization") String auth, @PathVariable String key, @RequestBody Map<String, Object> body);

    @PostMapping(value = "/issue/{key}/watchers", consumes = MediaType.APPLICATION_JSON_VALUE)
    void addWatcher(@RequestHeader("Authorization") String auth, @PathVariable String key, @RequestBody String usernameOrAccountId);

    @PostMapping(value = "/issue/{key}/votes")
    void addVote(@RequestHeader("Authorization") String auth, @PathVariable String key);

    @DeleteMapping(value = "/issue/{key}/votes")
    void removeVote(@RequestHeader("Authorization") String auth, @PathVariable String key);

    @GetMapping("/search")
    Map<String, Object> search(@RequestHeader("Authorization") String auth, @RequestParam String jql,
                               @RequestParam(defaultValue = "0") int startAt, @RequestParam(defaultValue = "50") int maxResults);

    @PutMapping(value = "/issue/{key}/assignee", consumes = MediaType.APPLICATION_JSON_VALUE)
    void assignIssue(@RequestHeader("Authorization") String auth, @PathVariable String key, @RequestBody JiraModels.AssigneePayload payload);

    @GetMapping(value = "/user/assignable/search", consumes = MediaType.APPLICATION_JSON_VALUE)
    List<JiraModels.AssignableUser> findAssignableUsers(
            @RequestHeader("Authorization") String auth,
            @RequestParam(required = false) String issueKey,
            @RequestParam(name = "project", required = false) String projectKey,
            @RequestParam(required = false) String username,
            @RequestParam(defaultValue = "0") int startAt,
            @RequestParam(defaultValue = "50") int maxResults
    );

    /**
     * POST /rest/api/2/issue/{issueIdOrKey}/attachments
     * Wymaga nagłówka: X-Atlassian-Token: no-check
     * Zwraca listę dodanych załączników (zwykle 1 plik => 1 element listy)
     */
    @PostMapping(
            value = "/issue/{issueKey}/attachments",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    List<JiraModels.Attachment> attachAndReturnMeta(
            @RequestHeader("Authorization") String auth,
            @RequestHeader("X-Atlassian-Token") String xsrf,
            @PathVariable("issueKey") String issueKey,
            @RequestPart("file") MultipartFile file
    );

    /**
     * GET /rest/api/2/attachment/{id} — metadane jednego załącznika.
     */
    @GetMapping("/attachment/{id}")
    JiraModels.Attachment getAttachment(
            @RequestHeader("Authorization") String auth,
            @PathVariable("id") String id
    );

    /**
     * GET /rest/api/2/issue/{key}/properties/{propertyKey}
     */
    @GetMapping("/issue/{key}/properties/{propertyKey}")
    Map<String, Object> getIssueProperty(
            @RequestHeader("Authorization") String auth,
            @PathVariable("key") String issueKey,
            @PathVariable("propertyKey") String propertyKey
    );

    /**
     * PUT /rest/api/2/issue/{key}/properties/{propertyKey} (body = raw property value JSON)
     */
    @PutMapping(value = "/issue/{key}/properties/{propertyKey}", consumes = MediaType.APPLICATION_JSON_VALUE)
    void setIssueProperty(
            @RequestHeader("Authorization") String auth,
            @PathVariable("key") String issueKey,
            @PathVariable("propertyKey") String propertyKey,
            @RequestBody Object propertyValue // ważne: to jest "value", bez wrappera
    );

    // GET /issue/{key}/comment
    @GetMapping(value = "/issue/{key}/comment", consumes = MediaType.APPLICATION_JSON_VALUE)
    JiraModels.CommentPage getComments(
            @RequestHeader("Authorization") String auth,
            @PathVariable("key") String issueKey
    );

    // POST /issue/{key}/comment  body = { "body": "..." }
    @PostMapping(value = "/issue/{key}/comment", consumes = MediaType.APPLICATION_JSON_VALUE)
    JiraModels.Comment addComment(
            @RequestHeader("Authorization") String auth,
            @PathVariable("key") String issueKey,
            @RequestBody Map<String, Object> body
    );

    // PUT /issue/{key}/comment/{id}
    @PutMapping(value = "/issue/{key}/comment/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    JiraModels.Comment updateComment(
            @RequestHeader("Authorization") String auth,
            @PathVariable("key") String issueKey,
            @PathVariable("id") String commentId,
            @RequestBody Map<String, Object> body
    );

    // DELETE /issue/{key}/comment/{id}
    @DeleteMapping("/issue/{key}/comment/{id}")
    void deleteComment(
            @RequestHeader("Authorization") String auth,
            @PathVariable("key") String issueKey,
            @PathVariable("id") String commentId
    );
}
