package com.example.consultingbooking.dto;

import com.example.consultingbooking.entity.SlotStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.List;

public final class SlotDtos {

    private SlotDtos() {
    }

    public record SlotRequest(
            @NotNull(message = "Start time is required") @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime startTime,
            @NotNull(message = "End time is required") @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime endTime
    ) {
    }

    public record SlotResponse(
            Long id,
            Long specialistId,
            String specialistName,
            LocalDateTime startTime,
            LocalDateTime endTime,
            SlotStatus status
    ) {
    }

    public enum ConflictPolicy {
        SKIP,
        OVERWRITE_AVAILABLE
    }

    public record RecurringSlotRequest(
            @NotNull(message = "Weekday is required") DayOfWeek dayOfWeek,
            @NotNull(message = "Start time is required") LocalTime startTime,
            @NotNull(message = "End time is required") LocalTime endTime,
            @NotNull(message = "Conflict policy is required") ConflictPolicy conflictPolicy
    ) {
    }

    public record RecurringSlotResponse(
            int requestedCount,
            int createdCount,
            int skippedCount,
            int replacedCount,
            List<SlotResponse> createdSlots
    ) {
    }
}
