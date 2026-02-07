package com.king.paysim.domain.user.dtos;

import java.time.LocalDateTime;

public record UserResponseDto(
        Long id,
        String firstName,
        String lastName,
        String email,
        String bvn,
        LocalDateTime created_at,
        LocalDateTime updated_at
) {
}
