package com.example.consultingbooking.dto;

import com.example.consultingbooking.entity.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public final class UserDtos {

    private UserDtos() {
    }

    public record CreateUserRequest(
            @NotBlank @Size(max = 50) String username,
            @NotBlank @Size(min = 6, max = 100) String password,
            @NotBlank @Size(max = 100) String fullName,
            @NotBlank @Email @Size(max = 120) String email,
            @Size(max = 30) String phone,
            @NotNull UserRole role
    ) {
    }

    public record UpdateProfileRequest(
            @NotBlank @Size(max = 100) String fullName,
            @NotBlank @Email @Size(max = 120) String email,
            @Size(max = 30) String phone
    ) {
    }

    public record AdminUpdateUserRequest(
            @NotBlank @Size(max = 50) String username,
            @NotBlank @Size(max = 100) String fullName,
            @NotBlank @Email @Size(max = 120) String email,
            @Size(max = 30) String phone,
            @NotNull UserRole role,
            @NotNull Boolean active
    ) {
    }

    public record UserResponse(
            Long id,
            String username,
            String fullName,
            String email,
            String phone,
            UserRole role,
            boolean active,
            LocalDateTime createdAt
    ) {
    }
}
