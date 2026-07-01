package com.king.paynith.domain.wallet.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateWalletDto(
        @NotBlank(message = "userId is required")
        String userId
) {
}
