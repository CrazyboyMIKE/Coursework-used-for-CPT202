package com.example.consultingbooking;

import com.example.consultingbooking.config.DataInitializer;
import java.lang.reflect.Field;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SeedDefinitionTest {

    @Test
    void shouldDefineInitialDemoAccounts() throws Exception {
        Assertions.assertEquals("AdminGroup28", readStaticString("ADMIN_USERNAME"));
        Assertions.assertEquals("Group28_CPT202", readStaticString("ADMIN_PASSWORD"));
        Assertions.assertEquals(10, readSeedList("CUSTOMER_SEEDS").size());
        Assertions.assertEquals(20, readSeedList("SPECIALIST_SEEDS").size());
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
}
