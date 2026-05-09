package com.example.consultingbooking.dto;

import java.time.LocalDateTime;
import java.util.Map;

public record ApiErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String code,
        String message,
        Map<String, String> fieldErrors
) {
}
