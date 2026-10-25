package com.hostel.mess.dto;

import jakarta.validation.constraints.Size;

public record ReviewRequest(
        @Size(max = 255) String remark
) {
}