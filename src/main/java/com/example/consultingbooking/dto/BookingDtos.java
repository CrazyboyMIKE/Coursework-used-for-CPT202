package com.example.consultingbooking.dto;

import com.example.consultingbooking.entity.BookingStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;

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

    public record RescheduleRequest(
            @NotNull @Positive Long newSlotId
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
            String feeCurrency,
            String lastActionReason,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }
}
