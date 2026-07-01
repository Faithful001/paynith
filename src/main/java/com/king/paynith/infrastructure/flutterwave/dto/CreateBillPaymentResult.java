package com.king.paynith.infrastructure.flutterwave.dto;

import java.math.BigDecimal;

public record CreateBillPaymentResult(
        FlutterwaveApiStatus status,
        String message,
        Object data
) {
    public record Data (
           String phone_number,
           BigDecimal amount,
           String network,
           String code,
           String tx_ref,
           String reference,
           String batch_reference,
           String recharge_token,
           BigDecimal fee
    ){}
}
