package com.hostel.mess.dto;

import java.time.LocalDate;

public record MealResponse(
        Long id,
        Long userId,
        String studentName,
        String roomNo,
        LocalDate mealDate,
        boolean breakfast,
        boolean lunch,
        boolean dinner,
        boolean onLeave
) {
}