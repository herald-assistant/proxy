package com.acme.herald.rating;

import com.acme.herald.domain.dto.RatingDtos;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "/rating", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "RatingController", description = "Rating operations exposed by the Provider proxy.")
public class RatingController {
    private final RatingService service;

    @GetMapping("/issues/{issueKey}/ratings/{ratingId}")
    @Operation(
            summary = "Fetch rating state for an issue",
            description = "Returns the current user's ratings and aggregated summary for the given issue and rating identifier."
    )
    public ResponseEntity<RatingDtos.RatingFetchRes> fetchRating(
            @Parameter(description = "Provider issue key.", example = "ABC-123")
            @PathVariable String issueKey,

            @Parameter(description = "Rating identifier.", example = "quality-v1")
            @PathVariable String ratingId
    ) {
        return ResponseEntity.ok(service.fetch(issueKey, ratingId));
    }

    @PostMapping(path = "/issues/{issueKey}/ratings/{ratingId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Create or update a rating value",
            description = "Upserts the current user's rating for a category. If value is null, the rating for that category is removed."
    )
    public ResponseEntity<RatingDtos.RatingFetchRes> upsertRating(
            @Parameter(description = "Provider issue key.", example = "ABC-123")
            @PathVariable String issueKey,

            @Parameter(description = "Rating identifier.", example = "quality-v1")
            @PathVariable String ratingId,

            @RequestBody @Valid RatingDtos.RatingUpsertReq body
    ) {
        return ResponseEntity.ok(service.upsert(issueKey, ratingId, body.catId(), body.value()));
    }
}
