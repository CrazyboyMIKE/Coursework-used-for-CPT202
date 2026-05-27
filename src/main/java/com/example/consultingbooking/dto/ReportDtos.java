package com.example.consultingbooking.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class ReportDtos {

    private ReportDtos() {
    }

    public record SummaryResponse(
            long totalUsers,
            long totalSpecialists,
            long totalAvailableSlots,
            long totalBookings,
            BigDecimal confirmedRevenue,
            Map<String, Long> bookingsByStatus
    ) {
    }

    public record EarningsEntry(
            Long bookingId,
            String customerName,
            String topic,
            LocalDateTime startTime,
            LocalDateTime endTime,
            long durationMinutes,
            BigDecimal unitPrice,
            BigDecimal pricingMultiplier,
            BigDecimal amount,
            String currency
    ) {
    }

    public record EarningsResponse(
            LocalDate fromDate,
            LocalDate toDate,
            BigDecimal totalEarnings,
            String currency,
            List<EarningsEntry> entries
    ) {
    }
}
