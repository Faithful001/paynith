package com.king.paysim.domain.wallet.dtos;

public record PaystackResponse(
        boolean status,
        String message,
        Data data
) {}

