package com.king.paysim.domain.wallet.dtos;

import jakarta.validation.constraints.NotBlank;

public record CreateWalletDto(
        @NotBlank(message = "userId is required")
        Long userId
) {
}
