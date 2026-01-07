package com.acme.herald.provider.feign;

import com.acme.herald.domain.JiraModels;
import com.acme.herald.domain.JiraModels.CreateIssueResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.JsonNode;

import java.util.List;

@FeignClient(name = "jiraV2", url = "${jira.baseUrl}", configuration = JiraFeignConfig.class)
public interface JiraApiV2Client {
    String REST_API_PREFIX = "/rest/api/2";
    String REST_PAT_PREFIX = "/rest/pat/latest";

    @PostMapping(value = REST_PAT_PREFIX + "/tokens", consumes = MediaType.APPLICATION_JSON_VALUE)
    JiraModels.JiraPatCreateResponse createPatToken(
            @RequestHeader("Authorization") String auth,
            @RequestBody JiraModels.JiraPatCreateRequest body
    );

    @DeleteMapping(value = REST_PAT_PREFIX + "/tokens/{id}")
    void revokePatToken(
            @RequestHeader("Authorization") String auth,
            @PathVariable("id") Long id
    );

    @GetMapping(value = REST_API_PREFIX + "/myself")
    JiraModels.UserResponse getMe(
            @RequestHeader("Authorization") String auth,
            @RequestParam("expand") String expand
    );

    @GetMapping(value = REST_API_PREFIX + "/mypermissions")
    JiraModels.PermissionsResponse myPermissions(
            @RequestHeader("Authorization") String auth,
            @RequestParam(required = false) String projectKey,
            @RequestParam(required = false) String issueKey,
            @RequestParam(required = false) String permissions
    );

    @GetMapping(REST_API_PREFIX + "/project/{projectKey}/properties/{propertyKey}")
    JsonNode getProjectProperty(
            @RequestHeader("Authorization") String auth,
            @PathVariable("projectKey") String projectKey,
            @PathVariable("propertyKey") String propertyKey
    );

    @PutMapping(
            value = REST_API_PREFIX + "/project/{projectKey}/properties/{propertyKey}",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    void setProjectProperty(
            @RequestHeader("Authorization") String auth,
            @PathVariable("projectKey") String projectKey,
            @PathVariable("propertyKey") String propertyKey,
            @RequestBody Object propertyValue
    );

    @PostMapping(value = REST_API_PREFIX + "/issue", consumes = MediaType.APPLICATION_JSON_VALUE)
    CreateIssueResponse createIssue(
            @RequestHeader("Authorization") String auth,
            @RequestBody Object body
    );

    @GetMapping(REST_API_PREFIX + "/issue/{key}")
    JsonNode getIssue(
            @RequestHeader("Authorization") String auth,
            @PathVariable String key,
            @RequestParam(required = false) String expand
    );

    @PutMapping(value = REST_API_PREFIX + "/issue/{key}", consumes = MediaType.APPLICATION_JSON_VALUE)
    void updateIssue(
            @RequestHeader("Authorization") String auth,
            @PathVariable String key,
            @RequestBody Object body
    );

    @PostMapping(value = REST_API_PREFIX + "/issue/{key}/watchers", consumes = MediaType.APPLICATION_JSON_VALUE)
    void addWatcher(
            @RequestHeader("Authorization") String auth,
            @PathVariable String key,
            @RequestBody String usernameOrAccountId
    );

    @PostMapping(value = REST_API_PREFIX + "/issue/{key}/votes")
    void addVote(@RequestHeader("Authorization") String auth, @PathVariable String key);

    @DeleteMapping(value = REST_API_PREFIX + "/issue/{key}/votes")
    void removeVote(@RequestHeader("Authorization") String auth, @PathVariable String key);

    @GetMapping(REST_API_PREFIX + "/search")
    JsonNode search(
            @RequestHeader("Authorization") String auth,
            @RequestParam String jql,
            @RequestParam(defaultValue = "0") int startAt,
            @RequestParam(defaultValue = "50") int maxResults
    );

    @PutMapping(value = REST_API_PREFIX + "/issue/{key}/assignee", consumes = MediaType.APPLICATION_JSON_VALUE)
    void assignIssue(
            @RequestHeader("Authorization") String auth,
            @PathVariable String key,
            @RequestBody JiraModels.AssigneePayload payload
    );

    @GetMapping(value = REST_API_PREFIX + "/issue/{key}/changelog")
    JiraModels.ChangelogPage getIssueChangelog(
            @RequestHeader("Authorization") String auth,
            @PathVariable("key") String key,
            @RequestParam(defaultValue = "0") int startAt,
            @RequestParam(defaultValue = "100") int maxResults
    );

    @GetMapping(value = REST_API_PREFIX + "/user/assignable/search")
    List<JiraModels.AssignableUser> findAssignableUsers(
            @RequestHeader("Authorization") String auth,
            @RequestParam(required = false) String issueKey,
            @RequestParam(name = "project", required = false) String projectKey,
            @RequestParam(required = false) String username,
            @RequestParam(defaultValue = "0") int startAt,
            @RequestParam(defaultValue = "50") int maxResults
    );

    @PostMapping(
            value = REST_API_PREFIX + "/issue/{issueKey}/attachments",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    List<JiraModels.Attachment> attachAndReturnMeta(
            @RequestHeader("Authorization") String auth,
            @RequestHeader("X-Atlassian-Token") String xsrf,
            @PathVariable("issueKey") String issueKey,
            @RequestPart("file") MultipartFile file
    );

    @GetMapping(REST_API_PREFIX + "/attachment/{id}")
    JiraModels.Attachment getAttachment(
            @RequestHeader("Authorization") String auth,
            @PathVariable("id") String id
    );

    @GetMapping(REST_API_PREFIX + "/issue/{key}/properties/{propertyKey}")
    JsonNode getIssueProperty(
            @RequestHeader("Authorization") String auth,
            @PathVariable("key") String issueKey,
            @PathVariable("propertyKey") String propertyKey
    );

    @PutMapping(
            value = REST_API_PREFIX + "/issue/{key}/properties/{propertyKey}",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    void setIssueProperty(
            @RequestHeader("Authorization") String auth,
            @PathVariable("key") String issueKey,
            @PathVariable("propertyKey") String propertyKey,
            @RequestBody Object propertyValue
    );

    @GetMapping(value = REST_API_PREFIX + "/issue/{key}/comment")
    JiraModels.CommentPage getComments(
            @RequestHeader("Authorization") String auth,
            @PathVariable("key") String issueKey
    );

    @PostMapping(value = REST_API_PREFIX + "/issue/{key}/comment", consumes = MediaType.APPLICATION_JSON_VALUE)
    JiraModels.Comment addComment(
            @RequestHeader("Authorization") String auth,
            @PathVariable("key") String issueKey,
            @RequestBody Object body
    );

    @PutMapping(value = REST_API_PREFIX + "/issue/{key}/comment/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    JiraModels.Comment updateComment(
            @RequestHeader("Authorization") String auth,
            @PathVariable("key") String issueKey,
            @PathVariable("id") String commentId,
            @RequestBody Object body
    );

    @DeleteMapping(REST_API_PREFIX + "/issue/{key}/comment/{id}")
    void deleteComment(
            @RequestHeader("Authorization") String auth,
            @PathVariable("key") String issueKey,
            @PathVariable("id") String commentId
    );

    @PostMapping(value = REST_API_PREFIX + "/issueLink", consumes = MediaType.APPLICATION_JSON_VALUE)
    void createIssueLink(
            @RequestHeader("Authorization") String auth,
            @RequestBody Object body
    );

    @GetMapping(value = REST_API_PREFIX + "/groups/picker")
    JsonNode groupPicker(
            @RequestHeader("Authorization") String auth,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String exclude,
            @RequestParam(defaultValue = "20") int maxResults
    );
}
