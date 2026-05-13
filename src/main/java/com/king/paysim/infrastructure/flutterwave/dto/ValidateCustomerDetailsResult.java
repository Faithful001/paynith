package com.king.paysim.infrastructure.flutterwave.dto;

public record ValidateCustomerDetailsResult(
        FlutterwaveApiStatus status,
        String message,
        Data data
) {
    private record Data (
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
