package com.king.paysim.infrastructure.flutterwave.dto;

import java.util.Map;

public record FlwTransactionResponse (
    String status,
    String message,
    FlwTransactionData data
){
    public record FlwTransactionData (
        String status,
        FlwChargeResponse.FlwCardData card,
        Map<String, Object> meta
    ){}
}

