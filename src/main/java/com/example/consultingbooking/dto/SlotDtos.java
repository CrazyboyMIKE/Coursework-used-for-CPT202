package com.example.consultingbooking.dto;

import com.example.consultingbooking.entity.SlotStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

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
}
