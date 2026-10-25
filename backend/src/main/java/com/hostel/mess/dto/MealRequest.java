package com.hostel.mess.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record MealRequest(
        @NotNull LocalDate mealDate,
        boolean breakfast,
        boolean lunch,
        boolean dinner
) {
}