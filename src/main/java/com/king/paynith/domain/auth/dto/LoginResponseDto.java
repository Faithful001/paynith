package com.king.paynith.domain.auth.dto;

import com.king.paynith.domain.user.dto.UserResponseDto;

public record LoginResponseDto (
        UserResponseDto user,
        String token
) { }
