package com.king.paysim.domain.user.dtos;

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
