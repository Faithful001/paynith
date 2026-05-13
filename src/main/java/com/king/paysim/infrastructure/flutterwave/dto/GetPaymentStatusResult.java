package com.king.paysim.infrastructure.flutterwave.dto;

import java.math.BigDecimal;

public record GetPaymentStatusResult(
        FlutterwaveApiStatus status,
        String message,
        Data data
) {
    private record Data (
            String tx_ref,
            BigDecimal amount,
            BigDecimal fee,
            String currency,
            String extra,
            String flw_ref,
            String token
    ){}
}
