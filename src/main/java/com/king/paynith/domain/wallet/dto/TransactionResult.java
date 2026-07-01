package com.king.paynith.domain.wallet.dto;

import com.king.paynith.domain.wallet.enums.WalletCurrency;
import com.king.paynith.domain.wallet.enums.WalletStatus;

import java.math.BigDecimal;

public record TransactionResult(
        String reference,
        BigDecimal amount,
        WalletCurrency currency,
        WalletStatus walletStatus
) {}