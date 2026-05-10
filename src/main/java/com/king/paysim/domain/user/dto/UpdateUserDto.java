package com.king.paysim.domain.user.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateUserDto(
        @NotBlank
        String firstName,

        @NotBlank
        String lastName,

        @NotBlank
        String phoneNumber,

        @NotBlank
        String bvn
) {
}
