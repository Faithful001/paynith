package com.king.paynith.domain.user.dto;

import java.time.LocalDateTime;

public record UserResponseDto(
        String id,
        String firstName,
        String lastName,
        String email,
        String bvn,
        String phoneNumber,
        LocalDateTime created_at,
        LocalDateTime updated_at
) {
}
