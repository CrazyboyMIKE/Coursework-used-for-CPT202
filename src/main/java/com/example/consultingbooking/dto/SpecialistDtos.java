package com.example.consultingbooking.dto;

import com.example.consultingbooking.entity.SpecialistLevel;
import com.example.consultingbooking.entity.SpecialistStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public final class SpecialistDtos {

    private SpecialistDtos() {
    }

    public record SpecialistRequest(
            @NotNull(message = "User ID is required") Long userId,
            @NotNull(message = "Category is required") Long categoryId,
            @NotNull(message = "Level is required") SpecialistLevel level,
            @NotNull(message = "Base fee is required") @DecimalMin(value = "0.0", message = "Base fee must be zero or greater") BigDecimal baseFee,
            @NotNull(message = "Status is required") SpecialistStatus status,
            @Size(max = 500) String bio
    ) {
    }

    public record SpecialistUpdateRequest(
            @NotNull(message = "Category is required") Long categoryId,
            @NotNull(message = "Level is required") SpecialistLevel level,
            @NotNull(message = "Base fee is required") @DecimalMin(value = "0.0", message = "Base fee must be zero or greater") BigDecimal baseFee,
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
            SpecialistLevel level,
            BigDecimal baseFee,
            SpecialistStatus status,
            String bio
    ) {
    }
}
