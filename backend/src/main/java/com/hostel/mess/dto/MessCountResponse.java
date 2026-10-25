package com.hostel.mess.dto;

import java.time.LocalDate;

public record MessCountResponse(
        LocalDate date,
        int breakfastCount,
        int lunchCount,
        int dinnerCount
) {
}