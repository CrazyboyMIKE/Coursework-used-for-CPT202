package com.example.consultingbooking.dto;

import com.example.consultingbooking.entity.RefundStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class RefundDtos {

    private RefundDtos() {
    }

    public record RefundResponse(
            Long bookingId,
            RefundStatus status,
            BigDecimal amount,
            String currency,
            String policyMessage,
            LocalDateTime synchronisedAt
    ) {
    }
}
