package com.king.paynith.infrastructure.flutterwave.dto;

import java.math.BigDecimal;

public record FlutterwaveData(
        String response_code,
        String response_message,
        String flw_ref,
        String order_ref,
        String account_number,
        String frequency,
        String bank_name,
        String created_at,
        String expiry_date,
        String note,
        BigDecimal amount
) {}
