package com.king.paysim.domain.virtual_account.dtos;

import java.util.Optional;

public record FlutterwaveVAResponse(
        FlutterwaveStatus status,
        Optional<String> message,
        Optional<FlutterwaveData> data,
        Optional<FlutterwaveError> error
) {
}
