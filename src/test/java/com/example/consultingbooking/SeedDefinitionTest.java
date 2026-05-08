package com.example.consultingbooking;

import com.example.consultingbooking.config.DataInitializer;
import com.example.consultingbooking.entity.BookingStatus;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SeedDefinitionTest {

    @Test
    void shouldDefineInitialDemoAccounts() throws Exception {
        Assertions.assertEquals("AdminGroup28", readStaticString("ADMIN_USERNAME"));
        Assertions.assertEquals("Group28_CPT202", readStaticString("ADMIN_PASSWORD"));
        Assertions.assertEquals(10, readSeedList("CUSTOMER_SEEDS").size());
        Assertions.assertEquals(20, readSeedList("SPECIALIST_SEEDS").size());

        List<?> bookingSeeds = readSeedList("BOOKING_SEEDS");
        Assertions.assertEquals(10, bookingSeeds.size());
        Assertions.assertEquals(
                EnumSet.allOf(BookingStatus.class),
                readBookingStatuses(bookingSeeds)
        );
    }

    private String readStaticString(String fieldName) throws Exception {
        Field field = DataInitializer.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return (String) field.get(null);
    }

    private List<?> readSeedList(String fieldName) throws Exception {
        Field field = DataInitializer.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return (List<?>) field.get(null);
    }

    private Set<BookingStatus> readBookingStatuses(List<?> bookingSeeds) throws Exception {
        Set<BookingStatus> statuses = EnumSet.noneOf(BookingStatus.class);
        for (Object seed : bookingSeeds) {
            Method method = seed.getClass().getDeclaredMethod("status");
            method.setAccessible(true);
            statuses.add((BookingStatus) method.invoke(seed));
        }
        return statuses;
    }
}
