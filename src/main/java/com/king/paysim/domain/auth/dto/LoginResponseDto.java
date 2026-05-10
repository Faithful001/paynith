package com.king.paysim.domain.auth.dto;

import com.king.paysim.domain.user.dto.UserResponseDto;

public record LoginResponseDto (
        UserResponseDto user,
        String token
) { }
