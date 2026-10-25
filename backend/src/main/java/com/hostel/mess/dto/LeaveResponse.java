package com.hostel.mess.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record LeaveResponse(
        Long id,
        Long userId,
        String studentName,
        String roomNo,
        LocalDate fromDate,
        LocalDate toDate,
        String reason,
        String status,
        String adminRemark,
        String reviewedByName,
        LocalDateTime createdAt,
        LocalDateTime reviewedAt
) {
}