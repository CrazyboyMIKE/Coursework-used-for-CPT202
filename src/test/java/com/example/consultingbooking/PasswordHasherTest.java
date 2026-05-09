package com.example.consultingbooking;

import com.example.consultingbooking.security.PasswordHasher;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PasswordHasherTest {

    @Test
    void shouldHashPasswordWithConfiguredSalt() {
        String hashed = PasswordHasher.hash("password123");

        Assertions.assertNotEquals("password123", hashed);
        Assertions.assertTrue(hashed.startsWith("sha256$" + PasswordHasher.SALT + "$"));
        Assertions.assertTrue(PasswordHasher.matches("password123", hashed));
        Assertions.assertFalse(PasswordHasher.matches("wrong-password", hashed));
    }

    @Test
    void shouldStillMatchLegacyPlainTextPasswordForUpgrade() {
        Assertions.assertTrue(PasswordHasher.matches("password123", "password123"));
        Assertions.assertTrue(PasswordHasher.needsHash("password123"));
    }
}
