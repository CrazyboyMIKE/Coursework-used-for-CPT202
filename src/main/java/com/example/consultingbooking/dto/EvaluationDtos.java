package com.example.consultingbooking.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public final class EvaluationDtos {

    private EvaluationDtos() {
    }

    public record SubmitEvaluationRequest(
            @Min(value = 1, message = "Rating must be between 1 and 5")
            @Max(value = 5, message = "Rating must be between 1 and 5")
            int rating,
            @NotBlank(message = "Comment is required")
            @Size(max = 500, message = "Comment must be 500 characters or fewer")
            String comment
    ) {
    }

    public record EvaluationResponse(
            Long id,
            Long bookingId,
            int rating,
            String comment,
            LocalDateTime createdAt
    ) {
    }
}
