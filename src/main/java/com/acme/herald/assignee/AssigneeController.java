package com.acme.herald.assignee;

import com.acme.herald.assignee.dto.AssigneeDtos;
import com.acme.herald.domain.JiraModels;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(path = "/users", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
@RequiredArgsConstructor
@Tag(name = "AssigneeController", description = "Assignee lookup and assignment operations exposed by the Provider proxy.")
public class AssigneeController {

    private final AssigneeService service;

    @GetMapping("/assignable")
    @Operation(
            summary = "Find users assignable to an issue",
            description = "Returns a paged list of users that can be assigned to the given Provider issue. "
                    + "When issueKey is provided, Provider can apply issue-specific permissions. "
                    + "Parameter 'username' is a text filter (login/name/email depending on Provider)."
    )
    public ResponseEntity<List<AssigneeDtos.AssignableUser>> findAssignableUsers(
            @RequestParam(required = false) String issueKey,
            @RequestParam(required = false, defaultValue = "") String username,
            @RequestParam(required = false, defaultValue = "0") @Min(0) int startAt,
            @RequestParam(required = false, defaultValue = "10") @Min(1) @Max(1000) int maxResults
    ) {
        var users = service.findAssignableUsers(issueKey, username, startAt, maxResults);
        return ResponseEntity.ok(users);
    }

    @PutMapping(path = "/assignee/{key}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Assign a user to an issue",
            description = "Sets the assignee for the given Provider issue key. "
                    + "Payload is Provider-specific."
    )
    public ResponseEntity<Void> assignIssue(
            @PathVariable String key,
            @RequestBody @Valid AssigneeDtos.AssigneeReq payload
    ) {
        service.assignIssue(key, payload);
        return ResponseEntity.noContent().build();
    }
}
