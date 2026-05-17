package com.king.paysim.infrastructure.flutterwave.dto;

public record FlwTokenizedChargeResponse(
    String status,
    String message,
    FlwChargeResponse.FlwChargeData data
) {
}
