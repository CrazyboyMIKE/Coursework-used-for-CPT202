package com.example.consultingbooking.dto;

import com.example.consultingbooking.entity.BookingStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class BookingDtos {

    private BookingDtos() {
    }

    public record CreateBookingRequest(
            @NotNull @Positive Long specialistId,
            @NotNull @Positive Long slotId,
            @NotBlank @Size(max = 120) String topic,
            @Size(max = 500) String notes
    ) {
    }

    public record ReasonRequest(
            @Size(max = 255) String reason
    ) {
    }

    public record RejectRequest(
            @NotBlank(message = "A rejection reason is required")
            @Size(min = 5, max = 255, message = "Rejection reason must be between 5 and 255 characters")
            String reason
    ) {
    }

    public record RescheduleRequest(
            @NotNull @Positive Long newSlotId
    ) {
    }

    public record QuoteRequest(
            @NotNull @Positive Long specialistId,
            @NotNull @Positive Long slotId
    ) {
    }

    public record BookingResponse(
            Long id,
            Long customerId,
            String customerName,
            Long specialistId,
            String specialistName,
            Long slotId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            BookingStatus status,
            String topic,
            String notes,
            BigDecimal price,
            BigDecimal unitPrice,
            BigDecimal pricingMultiplier,
            long durationMinutes,
            String feeCurrency,
            String lastActionReason,
            LocalDateTime cancelledAt,
            boolean evaluationSubmitted,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    public record FeeBreakdownResponse(
            Long bookingId,
            String specialistName,
            String topic,
            LocalDateTime startTime,
            LocalDateTime endTime,
            long durationMinutes,
            BigDecimal unitPrice,
            BigDecimal pricingMultiplier,
            BigDecimal totalPrice,
            String feeCurrency,
            List<FeeSegmentResponse> components
    ) {
    }

    public record FeeQuoteResponse(
            Long specialistId,
            String specialistName,
            Long slotId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            long durationMinutes,
            BigDecimal unitPrice,
            BigDecimal pricingMultiplier,
            BigDecimal totalPrice,
            String feeCurrency,
            List<FeeSegmentResponse> components
    ) {
    }

    public record FeeSegmentResponse(
            String label,
            LocalDateTime startTime,
            LocalDateTime endTime,
            long durationMinutes,
            BigDecimal multiplier,
            BigDecimal amount
    ) {
    }
}
