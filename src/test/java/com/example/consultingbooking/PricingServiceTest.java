package com.example.consultingbooking;

import com.example.consultingbooking.entity.SpecialistProfile;
import com.example.consultingbooking.entity.TimeSlot;
import com.example.consultingbooking.service.PricingService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PricingServiceTest {

    private final PricingService pricingService = new PricingService();

    @Test
    void calculatesWeekdayHourlyPrice() {
        SpecialistProfile specialist = specialistWithBaseFee("300.00");
        TimeSlot slot = slotAt(LocalDateTime.of(2026, 5, 11, 10, 0), 60);

        BigDecimal price = pricingService.calculatePrice(specialist, slot);

        Assertions.assertEquals(new BigDecimal("300.00"), price);
    }

    @Test
    void calculatesWeekendSurcharge() {
        SpecialistProfile specialist = specialistWithBaseFee("200.00");
        TimeSlot slot = slotAt(LocalDateTime.of(2026, 5, 9, 10, 0), 60);

        BigDecimal price = pricingService.calculatePrice(specialist, slot);

        Assertions.assertEquals(new BigDecimal("230.00"), price);
    }

    @Test
    void roundsPartialHourPriceToTwoDecimals() {
        SpecialistProfile specialist = specialistWithBaseFee("99.99");
        TimeSlot slot = slotAt(LocalDateTime.of(2026, 5, 11, 10, 0), 30);

        BigDecimal price = pricingService.calculatePrice(specialist, slot);

        Assertions.assertEquals(new BigDecimal("50.00"), price);
    }

    @Test
    void appliesSegmentedPricingWhenSlotCrossesIntoWeekend() {
        SpecialistProfile specialist = specialistWithBaseFee("300.00");
        TimeSlot slot = slotAt(LocalDateTime.of(2026, 5, 8, 23, 30), 60);

        PricingService.PriceCalculation calculation = pricingService.calculateBreakdown(specialist.getBaseFee(), slot);

        Assertions.assertEquals(new BigDecimal("322.50"), calculation.totalPrice());
        Assertions.assertEquals(new BigDecimal("1.08"), calculation.effectiveMultiplier());
        Assertions.assertEquals(2, calculation.segments().size());
        Assertions.assertEquals("Standard rate", calculation.segments().get(0).label());
        Assertions.assertEquals(new BigDecimal("150.00"), calculation.segments().get(0).amount());
        Assertions.assertEquals("Weekend rate", calculation.segments().get(1).label());
        Assertions.assertEquals(new BigDecimal("172.50"), calculation.segments().get(1).amount());
    }

    private SpecialistProfile specialistWithBaseFee(String baseFee) {
        SpecialistProfile specialist = new SpecialistProfile();
        specialist.setBaseFee(new BigDecimal(baseFee));
        return specialist;
    }

    private TimeSlot slotAt(LocalDateTime startTime, int minutes) {
        TimeSlot slot = new TimeSlot();
        slot.setStartTime(startTime);
        slot.setEndTime(startTime.plusMinutes(minutes));
        return slot;
    }
}
