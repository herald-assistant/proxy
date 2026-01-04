package com.acme.herald.vote;

import com.acme.herald.domain.dto.VoteDtos;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "/vote", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "VoteController", description = "Lightweight per-issue voting stored in Provider issue properties.")
public class VoteController {

    private final VoteService service;

    @GetMapping("/issues/{issueKey}/votes/{voteId}")
    @Operation(
            summary = "Fetch votes for a given issue and vote widget",
            description = "Returns the current user's vote direction and the aggregated vote summary for the given Provider issue key and voteId."
    )
    public ResponseEntity<VoteDtos.VoteFetchRes> fetchVotes(
            @PathVariable String issueKey,
            @PathVariable String voteId
    ) {
        return ResponseEntity.ok(service.fetch(issueKey, voteId));
    }

    @PostMapping(path = "/issues/{issueKey}/votes/{voteId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Upsert current user's vote",
            description = "Sets the current user's vote direction for the given voteId. "
                    + "Use dir='up' or dir='down' to vote, or dir=null to remove the vote."
    )
    public ResponseEntity<VoteDtos.VoteFetchRes> upsertVotes(
            @PathVariable String issueKey,
            @PathVariable String voteId,
            @RequestBody @Valid VoteDtos.VoteUpsertReq body
    ) {
        return ResponseEntity.ok(service.upsert(issueKey, voteId, body.dir()));
    }
}
