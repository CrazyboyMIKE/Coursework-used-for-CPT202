package com.example.consultingbooking;

import com.example.consultingbooking.exception.BusinessException;
import com.example.consultingbooking.service.TextNormalizer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TextNormalizerTest {

    @Test
    void cleanOptionalTrimsBlankValuesToNull() {
        Assertions.assertNull(TextNormalizer.cleanOptional(null));
        Assertions.assertNull(TextNormalizer.cleanOptional("   "));
        Assertions.assertEquals("hello", TextNormalizer.cleanOptional("  hello  "));
    }

    @Test
    void cleanRequiredRejectsBlankValues() {
        BusinessException exception = Assertions.assertThrows(
                BusinessException.class,
                () -> TextNormalizer.cleanRequired(" ", "Value is required")
        );

        Assertions.assertEquals("Value is required", exception.getMessage());
    }

    @Test
    void keywordTrimsAndLowercasesValues() {
        Assertions.assertNull(TextNormalizer.keyword(null));
        Assertions.assertNull(TextNormalizer.keyword("   "));
        Assertions.assertEquals("legal consulting", TextNormalizer.keyword("  Legal Consulting  "));
    }
}
