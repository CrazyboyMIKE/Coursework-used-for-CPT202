package com.example.consultingbooking.service;

import com.example.consultingbooking.entity.SpecialistProfile;
import com.example.consultingbooking.entity.TimeSlot;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PricingService {

    public BigDecimal calculatePrice(SpecialistProfile specialist, TimeSlot slot) {
        return calculateBreakdown(specialist.getBaseFee(), slot).totalPrice();
    }

    public BigDecimal pricingMultiplier(TimeSlot slot) {
        return calculateBreakdown(BigDecimal.ONE, slot).effectiveMultiplier();
    }

    public PriceCalculation calculateBreakdown(BigDecimal unitPrice, TimeSlot slot) {
        long durationMinutes = Duration.between(slot.getStartTime(), slot.getEndTime()).toMinutes();
        List<PriceSegment> segments = new ArrayList<>();
        LocalDateTime segmentStart = slot.getStartTime();

        while (segmentStart.isBefore(slot.getEndTime())) {
            LocalDateTime nextDay = segmentStart.toLocalDate().plusDays(1).atStartOfDay();
            LocalDateTime segmentEnd = nextDay.isBefore(slot.getEndTime()) ? nextDay : slot.getEndTime();
            long segmentMinutes = Duration.between(segmentStart, segmentEnd).toMinutes();
            BigDecimal multiplier = multiplierFor(segmentStart.getDayOfWeek());
            BigDecimal amount = unitPrice
                    .multiply(BigDecimal.valueOf(segmentMinutes))
                    .multiply(multiplier)
                    .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);

            segments.add(new PriceSegment(
                    isWeekend(segmentStart.getDayOfWeek()) ? "Weekend rate" : "Standard rate",
                    segmentStart,
                    segmentEnd,
                    segmentMinutes,
                    multiplier,
                    amount
            ));
            segmentStart = segmentEnd;
        }

        BigDecimal totalPrice = segments.stream()
                .map(PriceSegment::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal weightedMinutes = segments.stream()
                .map(segment -> BigDecimal.valueOf(segment.durationMinutes()).multiply(segment.multiplier()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal effectiveMultiplier = durationMinutes == 0
                ? BigDecimal.ONE.setScale(2)
                : weightedMinutes.divide(BigDecimal.valueOf(durationMinutes), 2, RoundingMode.HALF_UP);

        return new PriceCalculation(unitPrice, durationMinutes, effectiveMultiplier, totalPrice, List.copyOf(segments));
    }

    private BigDecimal multiplierFor(DayOfWeek dayOfWeek) {
        return isWeekend(dayOfWeek) ? new BigDecimal("1.15") : BigDecimal.ONE.setScale(2);
    }

    private boolean isWeekend(DayOfWeek dayOfWeek) {
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }

    public record PriceSegment(
            String label,
            LocalDateTime startTime,
            LocalDateTime endTime,
            long durationMinutes,
            BigDecimal multiplier,
            BigDecimal amount
    ) {
    }

    public record PriceCalculation(
            BigDecimal unitPrice,
            long durationMinutes,
            BigDecimal effectiveMultiplier,
            BigDecimal totalPrice,
            List<PriceSegment> segments
    ) {
    }
}
