package com.king.paysim.infrastructure.flutterwave.dto;

public record VerifyBankAccountResult(
        FlutterwaveApiStatus status,
        String message,
        Data data
) {
    private record Data (
            String account_number,
            String account_name
    ){}
}
