package com.hostel.mess.dto;

import java.math.BigDecimal;

public record RatesResponse(
        BigDecimal breakfastRate,
        BigDecimal lunchRate,
        BigDecimal dinnerRate,
        int lowStockThreshold
) {
}