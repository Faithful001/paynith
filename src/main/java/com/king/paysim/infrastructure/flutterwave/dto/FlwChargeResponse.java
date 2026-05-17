package com.king.paysim.infrastructure.flutterwave.dto;

import com.king.paysim.domain.wallet.enums.WalletCurrency;

import java.math.BigDecimal;
import java.util.Map;

public record FlwChargeResponse(
        String status,
        String message,
        FlwChargeData data
) {
    public record FlwChargeData(
        String id,
        String tx_ref,
        String flw_ref,
        String status,
        BigDecimal amount,
        BigDecimal charged_amount,
        String currency,
        FlwCardData card,
        Map<String, Object> meta
        ){}

    public record FlwCardData (
        String token,
        String last4digits,
        String expiry,
        String brand,
        String type
    ){}
}
