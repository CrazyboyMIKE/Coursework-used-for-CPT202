package com.example.consultingbooking.dto;

import com.example.consultingbooking.entity.SpecialistStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public final class SpecialistDtos {

    private SpecialistDtos() {
    }

    public record SpecialistRequest(
            @NotNull(message = "User ID is required") @Positive Long userId,
            @NotNull(message = "Category is required") @Positive Long categoryId,
            @NotBlank(message = "Professional title or certification is required") @Size(max = 120) String level,
            @NotNull(message = "Base fee is required") @DecimalMin(value = "0.0", message = "Base fee must be zero or greater") BigDecimal baseFee,
            String feeCurrency,
            @NotNull(message = "Status is required") SpecialistStatus status,
            @Size(max = 500) String bio
    ) {
    }

    public record SpecialistUpdateRequest(
            @NotNull(message = "Category is required") @Positive Long categoryId,
            @NotBlank(message = "Professional title or certification is required") @Size(max = 120) String level,
            @NotNull(message = "Base fee is required") @DecimalMin(value = "0.0", message = "Base fee must be zero or greater") BigDecimal baseFee,
            String feeCurrency,
            @NotNull(message = "Status is required") SpecialistStatus status,
            @Size(max = 500) String bio
    ) {
    }

    public record SpecialistResponse(
            Long id,
            Long userId,
            Long categoryId,
            String fullName,
            String categoryName,
            String level,
            BigDecimal baseFee,
            String feeCurrency,
            SpecialistStatus status,
            String bio
    ) {
    }
}
