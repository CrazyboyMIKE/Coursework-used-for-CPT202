package com.example.consultingbooking.dto;

import com.example.consultingbooking.entity.NotificationType;
import java.time.LocalDateTime;

public final class NotificationDtos {

    private NotificationDtos() {
    }

    public record NotificationResponse(
            Long id,
            Long bookingId,
            NotificationType type,
            String title,
            String message,
            boolean read,
            LocalDateTime createdAt
    ) {
    }
}
