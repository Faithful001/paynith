package com.king.paysim.domain.wallet.dto;

import com.king.paysim.domain.wallet.enums.WalletCurrency;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.Optional;

@Builder
public record WithdrawalDto(
        @NotBlank
        String accountBank,

        @NotBlank
        String accountNumber,

        @NotBlank
        String bankCode,

        @NotBlank
        String accountName,

        @NotNull
        WalletCurrency currency,

        @NotNull
        @Positive
        BigDecimal amount,

        @NotBlank
        String reference,

        String narration
) {
}