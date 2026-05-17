package com.king.paysim.domain.payment.dto;

import java.math.BigDecimal;
import java.util.Map;

public record PaymentResponse(
        String status,
        String message,
        String flwRef,
        String txRef,
        BigDecimal amount,
        String currency,
        Map<String, Object> data
) {
    public static PaymentResponse success(String txRef, BigDecimal amount) {
        return new PaymentResponse("success", "Transaction successful", null, txRef, amount, "NGN", null);
    }

    public static PaymentResponse pending(String txRef, BigDecimal amount) {
        return new PaymentResponse("pending", "Transaction initiated", null, txRef, amount, "NGN", null);
    }
}