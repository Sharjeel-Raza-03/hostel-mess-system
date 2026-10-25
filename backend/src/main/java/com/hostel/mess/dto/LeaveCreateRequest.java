package com.hostel.mess.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record LeaveCreateRequest(
        @NotNull LocalDate fromDate,
        @NotNull LocalDate toDate,
        @NotBlank @Size(max = 255) String reason
) {
}