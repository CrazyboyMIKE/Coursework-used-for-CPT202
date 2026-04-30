package com.example.consultingbooking.service;

import com.example.consultingbooking.entity.SpecialistLevel;
import com.example.consultingbooking.entity.SpecialistProfile;
import com.example.consultingbooking.entity.TimeSlot;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Duration;
import org.springframework.stereotype.Service;

@Service
public class PricingService {

    public BigDecimal calculatePrice(SpecialistProfile specialist, TimeSlot slot) {
        long minutes = Duration.between(slot.getStartTime(), slot.getEndTime()).toMinutes();
        BigDecimal hours = BigDecimal.valueOf(minutes)
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        BigDecimal amount = specialist.getBaseFee().multiply(hours);

        amount = amount.multiply(levelMultiplier(specialist.getLevel()));
        if (isWeekend(slot)) {
            amount = amount.multiply(BigDecimal.valueOf(1.15));
        }

        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal levelMultiplier(SpecialistLevel level) {
        return switch (level) {
            case JUNIOR -> BigDecimal.valueOf(1.00);
            case MID -> BigDecimal.valueOf(1.05);
            case SENIOR -> BigDecimal.valueOf(1.10);
            case PRINCIPAL -> BigDecimal.valueOf(1.20);
        };
    }

    private boolean isWeekend(TimeSlot slot) {
        DayOfWeek dayOfWeek = slot.getStartTime().getDayOfWeek();
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }
}
