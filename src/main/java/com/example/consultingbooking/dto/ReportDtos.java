package com.example.consultingbooking.dto;

import java.math.BigDecimal;
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
}
