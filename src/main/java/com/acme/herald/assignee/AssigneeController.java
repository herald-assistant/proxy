package com.acme.herald.assignee;

import com.acme.herald.domain.JiraModels;
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
public class AssigneeController {
    private final AssigneeService service;

    /**
     * Zwraca użytkowników, których można przypisać do danego issue.
     * Parametry:
     * - issueKey: klucz issue (preferowane – dokładniejsze uprawnienia)
     * - query: filtr tekstowy (login, imię, email)
     * - startAt/maxResults: stronicowanie
     */
    @GetMapping("/assignable")
    public ResponseEntity<List<JiraModels.AssignableUser>> findAssignableUsers(
            @RequestParam(required = false) String issueKey,
            @RequestParam(required = false, defaultValue = "") String username,
            @RequestParam(required = false, defaultValue = "0") @Min(0) int startAt,
            @RequestParam(required = false, defaultValue = "10") @Min(1) @Max(1000) int maxResults
    ) {
        var users = service.findAssignableUsers(issueKey, username, startAt, maxResults);
        return ResponseEntity.ok(users);
    }

    /**
     * Ustawia assignee dla issue
     */
    @PutMapping(path = "/assignee/{key}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> assignIssue(
            @PathVariable String key,
            @RequestBody JiraModels.AssigneePayload payload
    ) {
        service.assignIssue(key, payload);
        return ResponseEntity.noContent().build();
    }
}