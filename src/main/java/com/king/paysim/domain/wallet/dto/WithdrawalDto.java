package com.king.paysim.domain.wallet.dto;

import com.king.paysim.domain.wallet.enums.WalletCurrency;

import java.math.BigDecimal;
import java.util.Optional;

public record WithdrawalDto(
        String accountBank,
        String accountNumber,
        String bankCode,
        String accountName,
        WalletCurrency currency,
        BigDecimal amount,
        String reference,
        Optional<String> narration
) {
}
