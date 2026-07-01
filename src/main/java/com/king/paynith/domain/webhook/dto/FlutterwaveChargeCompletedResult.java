package com.king.paynith.domain.webhook.dto;

import com.king.paynith.domain.wallet.enums.WalletCurrency;

import java.math.BigDecimal;

public record FlutterwaveChargeCompletedResult(
        String id,
        String tx_ref,
        String flw_ref,
        BigDecimal amount,
        WalletCurrency currency,
        BigDecimal charged_amount,
        String status,
        String payment_type,
        String created_at,
        Customer customer
) {
    public record Customer(
            String id,
            String name,
            String phone_number,
            String email,
            String created_at
    ) {}
}