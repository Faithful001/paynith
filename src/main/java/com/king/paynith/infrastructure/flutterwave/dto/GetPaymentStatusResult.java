package com.king.paynith.infrastructure.flutterwave.dto;

import java.math.BigDecimal;

public record GetPaymentStatusResult(
        FlutterwaveApiStatus status,
        String message,
        Data data
) {
    public record Data (
            String tx_ref,
            BigDecimal amount,
            BigDecimal fee,
            String currency,
            String extra,
            String flw_ref,
            String token
    ){}
}
