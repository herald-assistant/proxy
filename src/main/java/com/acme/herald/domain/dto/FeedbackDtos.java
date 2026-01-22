package com.acme.herald.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class FeedbackDtos {

    public enum FeedbackType {
        BUG, IDEA
    }

    public record Feedback(
            String id,
            String type,        // "BUG" | "IDEA" (trzymamy jako string w odpowiedzi)
            String status,      // "TODO" | "IN_PROGRESS" | "DONE" | "REJECTED"
            String summary,
            String description,
            String authorKey,
            String authorDisplayName,
            String createdAt,
            String updatedAt
    ) {}

    public record CreateFeedbackReq(
            @NotBlank @Size(min = 3, max = 120) String summary,
            @Size(max = 8000) String description,
            @NotBlank String type // BUG | IDEA (string, bo FE będzie wysyłał string)
    ) {}

    /**
     * summary/description mogą być null -> "nie zmieniaj".
     * status można zmieniać tylko adminem (serwis to egzekwuje).
     */
    public record UpdateFeedbackReq(
            @Size(min = 3, max = 120) String summary,
            @Size(max = 8000) String description,
            String status
    ) {}

    public record FeedbackStats(
            long total,
            long bugs,
            long ideas,
            long todo,
            long inProgress,
            long done,
            long rejected
    ) {}
}
