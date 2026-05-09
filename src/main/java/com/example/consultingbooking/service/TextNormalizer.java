package com.example.consultingbooking.service;

import com.example.consultingbooking.exception.BusinessException;
import org.springframework.http.HttpStatus;

public final class TextNormalizer {

    private TextNormalizer() {
    }

    public static String cleanOptional(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static String cleanRequired(String value, String message) {
        String cleaned = cleanOptional(value);
        if (cleaned == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, message);
        }
        return cleaned;
    }

    public static String keyword(String value) {
        String cleaned = cleanOptional(value);
        return cleaned == null ? null : cleaned.toLowerCase();
    }
}
