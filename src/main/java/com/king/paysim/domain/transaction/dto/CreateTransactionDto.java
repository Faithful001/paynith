package com.king.paysim.domain.transaction.dto;

import com.king.paysim.domain.transaction.enums.TransactionStatus;
import com.king.paysim.domain.transaction.enums.TransactionType;
import com.king.paysim.domain.wallet.enums.WalletCurrency;
import jakarta.annotation.Nullable;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.Optional;

@Builder
public record CreateTransactionDto(
        BigDecimal amount,
        WalletCurrency currency,
        String walletId,
        TransactionType transactionType,
        String providerRef,
        String reference,
        @Nullable TransactionStatus status,
        String narration,
        String recipientAccountNumber,
        String recipientBankName,
        String recipientAccountName,
        BigDecimal fee
) {
}
