package com.king.paysim.domain.webhook.dtos;

import com.king.paysim.domain.wallet.enums.WalletCurrency;

public record FlutterwaveChargeCompletedDto(
        String id,
        String tx_ref,
        String flw_ref,
        long amount,
        WalletCurrency currency,
        String charged_amount,
        String status,
        String payment_type,
        String created_at,
        Customer customer
) {
    public record Customer(
            long id,
            String name,
            String phone_number,
            String email,
            String created_at
    ) {}
}