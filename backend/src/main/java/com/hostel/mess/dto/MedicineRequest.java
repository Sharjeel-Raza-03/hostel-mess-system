package com.hostel.mess.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record MedicineRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 50) String category,
        @Size(max = 255) String description,
        @NotNull @Min(0) Integer quantity,
        @Size(max = 20) String unit,
        @NotNull LocalDate expiryDate
) {
}