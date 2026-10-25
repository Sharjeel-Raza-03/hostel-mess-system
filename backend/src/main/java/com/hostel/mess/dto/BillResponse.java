package com.hostel.mess.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BillResponse(
        Long id,
        Long userId,
        String studentName,
        String roomNo,
        Integer billMonth,
        Integer billYear,
        Integer totalMeals,
        Integer leaveDays,
        BigDecimal mealRate,
        BigDecimal totalAmount,
        LocalDateTime generatedAt
) {
}