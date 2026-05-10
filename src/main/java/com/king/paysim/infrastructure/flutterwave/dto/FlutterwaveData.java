package com.king.paysim.infrastructure.flutterwave.dto;

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
        String amount
) {}
