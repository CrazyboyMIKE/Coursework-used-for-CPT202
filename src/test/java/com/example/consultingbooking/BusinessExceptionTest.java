package com.example.consultingbooking;

import com.example.consultingbooking.exception.BusinessException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class BusinessExceptionTest {

    @Test
    void existingConstructorKeepsStatusAndMessageWithNoCode() {
        BusinessException exception = new BusinessException(HttpStatus.BAD_REQUEST, "Invalid input");

        Assertions.assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        Assertions.assertEquals("Invalid input", exception.getMessage());
        Assertions.assertNull(exception.getCode());
    }

    @Test
    void codeConstructorKeepsStableErrorCode() {
        BusinessException exception = new BusinessException(
                HttpStatus.FORBIDDEN,
                "ACCESS_DENIED",
                "Access denied"
        );

        Assertions.assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        Assertions.assertEquals("ACCESS_DENIED", exception.getCode());
        Assertions.assertEquals("Access denied", exception.getMessage());
    }
}
