package com.king.paysim.domain.wallet.dto;

import com.king.paysim.domain.wallet.enums.WalletCurrency;
import com.king.paysim.domain.wallet.enums.WalletStatus;

import java.math.BigDecimal;

public record TransactionResult(
        String reference,
        BigDecimal amount,
        WalletCurrency currency,
        WalletStatus walletStatus
) {}