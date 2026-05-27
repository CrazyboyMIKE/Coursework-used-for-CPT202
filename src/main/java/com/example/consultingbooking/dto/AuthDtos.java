package com.example.consultingbooking.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Size(max = 50) String username,
            @NotBlank @Size(min = 8, max = 20, message = "Password must be between 8 and 20 characters") String password,
            @NotBlank @Size(max = 100) String fullName,
            @NotBlank @Email @Size(max = 120) String email,
            @Size(max = 30) String phone
    ) {
    }

    public record SpecialistRegisterRequest(
            @NotBlank @Size(max = 50) String username,
            @NotBlank @Size(min = 8, max = 20, message = "Password must be between 8 and 20 characters") String password,
            @NotBlank @Size(max = 100) String fullName,
            @NotBlank @Email @Size(max = 120) String email,
            @Size(max = 30) String phone,
            @NotBlank @Size(max = 80) String categoryName,
            @NotBlank @Size(max = 120) String level,
            @NotNull @DecimalMin(value = "0.01", message = "Base fee must be greater than zero") BigDecimal baseFee,
            String feeCurrency,
            @NotBlank(message = "Notes are required") @Size(max = 500) String bio
    ) {
    }

    public record LoginRequest(
            @NotBlank @Size(max = 120) String username,
            @NotBlank @Size(max = 255) String password
    ) {
    }

    public record AuthResponse(
            Long userId,
            String username,
            String fullName,
            String role,
            String token
    ) {
    }

    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = 8, max = 20, message = "Password must be between 8 and 20 characters") String newPassword
    ) {
    }

    public record PasswordResetRequest(
            @NotBlank @Size(max = 120) String identifier
    ) {
    }

    public record PasswordResetRequestResponse(
            String message,
            String resetCode,
            LocalDateTime expiresAt
    ) {
    }

    public record PasswordResetConfirmRequest(
            @NotBlank @Size(max = 32) String resetCode,
            @NotBlank @Size(min = 8, max = 20, message = "Password must be between 8 and 20 characters") String newPassword
    ) {
    }
}
