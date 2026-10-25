package com.hostel.mess.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;

public record RatesRequest(
        @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal breakfastRate,
        @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal lunchRate,
        @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal dinnerRate,
        @Min(0) Integer lowStockThreshold
) {
}