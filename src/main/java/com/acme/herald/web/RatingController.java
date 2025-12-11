package com.acme.herald.web;

import com.acme.herald.domain.dto.RatingDtos;
import com.acme.herald.service.RatingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping(path = "/rating", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class RatingController {
    private final RatingService service;

    @GetMapping("/issues/{issueKey}/ratings/{ratingId}")
    public ResponseEntity<RatingDtos.RatingFetchRes> fetchRating(@PathVariable String issueKey,
                                                           @PathVariable String ratingId) {
        return ResponseEntity.ok(service.fetch(issueKey, ratingId));
    }

    @PostMapping(path = "/issues/{issueKey}/ratings/{ratingId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RatingDtos.RatingFetchRes> upsertRating(@PathVariable String issueKey,
                                                            @PathVariable String ratingId,
                                                            @RequestBody RatingDtos.RatingUpsertReq body) {
        return ResponseEntity.ok(service.upsert(issueKey, ratingId, body.catId(), body.value()));
    }
}
