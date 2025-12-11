// package com.acme.herald.web;
package com.acme.herald.web;

import com.acme.herald.domain.dto.VoteDtos;
import com.acme.herald.service.VoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "/vote", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class VoteController {
    private final VoteService service;

    @GetMapping("/issues/{issueKey}/votes/{voteId}")
    public ResponseEntity<VoteDtos.VoteFetchRes> fetchVotes(@PathVariable String issueKey,
                                                       @PathVariable String voteId) {
        return ResponseEntity.ok(service.fetch(issueKey, voteId));
    }

    @PostMapping(path = "/issues/{issueKey}/votes/{voteId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<VoteDtos.VoteFetchRes> upsertVotes(@PathVariable String issueKey,
                                                        @PathVariable String voteId,
                                                        @RequestBody VoteDtos.VoteUpsertReq body) {
        return ResponseEntity.ok(service.upsert(issueKey, voteId, body.dir()));
    }
}
