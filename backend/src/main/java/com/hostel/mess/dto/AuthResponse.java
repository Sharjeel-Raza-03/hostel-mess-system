package com.hostel.mess.dto;

public record AuthResponse(
        String token,
        String role,
        Long userId,
        String fullName,
        String email
) {
}