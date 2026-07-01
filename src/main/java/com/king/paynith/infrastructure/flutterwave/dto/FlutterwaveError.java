package com.king.paynith.infrastructure.flutterwave.dto;

import java.util.List;
import java.util.Optional;

public record FlutterwaveError(
        String type,
        String code,
        String message,
        Optional<List<FlutterwaveValidationError>> validation_errors
) {
}
