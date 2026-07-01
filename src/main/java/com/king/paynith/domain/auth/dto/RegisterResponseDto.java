package com.king.paynith.domain.auth.dto;

import com.king.paynith.domain.user.dto.UserResponseDto;

public record RegisterResponseDto(
        UserResponseDto user
) {
}
