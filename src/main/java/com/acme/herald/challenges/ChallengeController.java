package com.acme.herald.challenges;

import com.acme.herald.domain.dto.ChallengeDtos;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/challenges", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "ChallengeController", description = "Template Hub Challenges operations.")
public class ChallengeController {

    private final ChallengeService service;

    @GetMapping
    @Operation(
            summary = "List challenges",
            description = "Everyone can read challenges. If challengesIssueKey is not configured, returns an empty list."
    )
    public List<ChallengeDtos.Challenge> listChallenges() {
        return service.list();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get challenge by id", description = "Everyone can read challenges.")
    public ChallengeDtos.Challenge getChallenge(@PathVariable String id) {
        return service.get(id);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Create challenge",
            description = "Anyone can create a challenge when challengesIssueKey is configured."
    )
    public ResponseEntity<ChallengeDtos.Challenge> createChallenge(@RequestBody @Valid ChallengeDtos.CreateChallengeReq req) {
        var created = service.create(req);
        return ResponseEntity.ok(created);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Update challenge",
            description = "Only the author of the challenge or a project admin can update it."
    )
    public ChallengeDtos.Challenge updateChallenge(
            @PathVariable String id,
            @RequestBody @Valid ChallengeDtos.UpdateChallengeReq req
    ) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete challenge",
            description = "Only the author of the challenge or a project admin can delete it."
    )
    public ResponseEntity<Void> deleteChallenge(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
