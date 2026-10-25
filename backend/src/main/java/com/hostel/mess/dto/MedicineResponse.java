package com.hostel.mess.dto;

import com.hostel.mess.model.MedicineStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record MedicineResponse(
        Long id,
        String name,
        String category,
        String description,
        Integer quantity,
        String unit,
        LocalDate expiryDate,
        MedicineStatus status,
        LocalDateTime updatedAt
) {
}