package com.king.paysim.domain.auth.dtos;

import com.king.paysim.domain.user.dtos.UserResponseDto;

public record LoginResponseDto (
        UserResponseDto user,
        String token
) { }
