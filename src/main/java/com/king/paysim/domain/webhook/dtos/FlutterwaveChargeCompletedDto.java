package com.king.paysim.domain.webhook.dtos;

public record FlutterwaveChargeCompletedDto(
        ChargeData data,
        String type
) {
    public record ChargeData(
            String id,
            long amount,
            String currency,
            String status,
            String reference,
            Customer customer
    ) {}

    public record Customer(
            String id,
            String email
    ) {}
}