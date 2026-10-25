package com.hostel.mess.dto;

import java.math.BigDecimal;

public record MonthlyReportResponse(
        int month,
        int year,
        int studentsBilled,
        int totalMeals,
        int totalLeaveDays,
        BigDecimal totalRevenue
) {
}