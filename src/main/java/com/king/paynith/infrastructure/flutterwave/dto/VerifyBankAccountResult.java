package com.king.paynith.infrastructure.flutterwave.dto;

public record VerifyBankAccountResult(
        FlutterwaveApiStatus status,
        String message,
        Data data
) {
    public record Data (
            String account_number,
            String account_name
    ){}
}
