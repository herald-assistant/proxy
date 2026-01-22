package com.acme.herald.feedback;

import com.acme.herald.domain.dto.FeedbackDtos;
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
@RequestMapping(path = "/feedback", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "FeedbackController", description = "Bug reports & ideas (stored as Jira issue properties).")
public class FeedbackController {

    private final FeedbackService service;

    @GetMapping
    @Operation(
            summary = "List feedback items (bugs & ideas)",
            description = "Everyone can read. If storage issue key is not configured, returns an empty list."
    )
    public List<FeedbackDtos.Feedback> listFeedbacks(
            @RequestParam(name = "type", required = false) String type,     // BUG/IDEA
            @RequestParam(name = "status", required = false) String status, // TODO/IN_PROGRESS/DONE/REJECTED
            @RequestParam(name = "mine", required = false, defaultValue = "false") boolean mine
    ) {
        return service.list(type, status, mine);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get feedback item by id", description = "Everyone can read.")
    public FeedbackDtos.Feedback getFeedback(@PathVariable String id) {
        return service.get(id);
    }

    @GetMapping("/stats")
    @Operation(summary = "Get simple stats", description = "Everyone can read.")
    public FeedbackDtos.FeedbackStats feedbacksStats() {
        return service.stats();
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Create feedback (bug/idea)",
            description = "Anyone can create when feedback storage is configured."
    )
    public ResponseEntity<FeedbackDtos.Feedback> createFeedback(@RequestBody @Valid FeedbackDtos.CreateFeedbackReq req) {
        var created = service.create(req);
        return ResponseEntity.ok(created);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Update feedback",
            description = "Only author or project admin can edit. Status can be changed only by admin."
    )
    public FeedbackDtos.Feedback updateFeedback(
            @PathVariable String id,
            @RequestBody @Valid FeedbackDtos.UpdateFeedbackReq req
    ) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete feedback",
            description = "Only author or project admin can delete."
    )
    public ResponseEntity<Void> deleteFeedback(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
