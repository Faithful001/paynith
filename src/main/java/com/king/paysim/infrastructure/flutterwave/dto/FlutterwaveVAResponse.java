package com.king.paysim.infrastructure.flutterwave.dto;

import java.util.Optional;

public record FlutterwaveVAResponse(
        FlutterwaveStatus status,
        Optional<String> message,
        Optional<FlutterwaveData> data,
        Optional<FlutterwaveError> error
) {
}
