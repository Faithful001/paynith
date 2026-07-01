package com.king.paynith.domain.wallet.dto;

import com.king.paynith.domain.wallet.enums.WalletCurrency;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record BillPaymentDto(
        BigDecimal amount,
        WalletCurrency currency,
        String reference,           // your tx_ref
        String flwRef,
        String customerId,
        String network,             // MTN, DSTV, etc.
        String itemCode,
        String billerCode,
        String narration,
        String status,
        String message
) {}