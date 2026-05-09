package com.king.paysim.domain.transaction.dtos;

import com.king.paysim.domain.transaction.enums.TransactionType;
import com.king.paysim.domain.wallet.enums.WalletCurrency;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.Optional;

@Builder
public record CreateTransactionDto(
        BigDecimal amount,
        WalletCurrency currency,
        String walletId,
        TransactionType transactionType,
        Optional<String> providerRef,
        Optional<String> reference,
        Optional<String> narration,
        Optional<String> recipientAccountNumber,
        Optional<String> recipientBankName,
        Optional<String> recipientAccountName,
        BigDecimal fee
) {
}
