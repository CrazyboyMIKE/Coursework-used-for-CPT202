package com.example.consultingbooking.dto;

import com.example.consultingbooking.entity.SpecialistLevel;
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
            @NotBlank @Size(min = 6, max = 100) String password,
            @NotBlank @Size(max = 100) String fullName,
            @NotBlank @Email @Size(max = 120) String email,
            @Size(max = 30) String phone
    ) {
    }

    public record SpecialistRegisterRequest(
            @NotBlank @Size(max = 50) String username,
            @NotBlank @Size(min = 6, max = 100) String password,
            @NotBlank @Size(max = 100) String fullName,
            @NotBlank @Email @Size(max = 120) String email,
            @Size(max = 30) String phone,
            @NotNull Long categoryId,
            @NotNull SpecialistLevel level,
            @NotNull @DecimalMin("0.0") BigDecimal baseFee,
            @Size(max = 500) String bio
    ) {
    }

    public record LoginRequest(
            @NotBlank String username,
            @NotBlank String password
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
            @NotBlank @Size(min = 6, max = 100) String newPassword
    ) {
    }

    public record PasswordResetRequest(
            @NotBlank String identifier
    ) {
    }

    public record PasswordResetRequestResponse(
            String message,
            String resetCode,
            LocalDateTime expiresAt
    ) {
    }

    public record PasswordResetConfirmRequest(
            @NotBlank String resetCode,
            @NotBlank @Size(min = 6, max = 100) String newPassword
    ) {
    }
}
