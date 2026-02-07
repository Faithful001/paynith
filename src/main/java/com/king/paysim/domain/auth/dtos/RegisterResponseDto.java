package com.king.paysim.domain.auth.dtos;

import com.king.paysim.domain.user.dtos.UserResponseDto;

public record RegisterResponseDto(
        UserResponseDto user
) {
}
