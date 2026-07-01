package com.king.paynith.infrastructure.flutterwave.dto;

public record ValidateCustomerDetailsResult(
        FlutterwaveApiStatus status,
        String message,
        Data data
) {
    public record Data (
            String response_code,
            String address,
            String response_message,
            String name,
            String biller_code,
            String customer,
            String product_code,
            String email,
            Integer fee,
            Integer maximum,
            Integer minimum
    ){}
}
