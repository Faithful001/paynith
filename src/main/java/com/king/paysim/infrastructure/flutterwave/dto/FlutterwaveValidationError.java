package com.king.paysim.infrastructure.flutterwave.dto;

public record FlutterwaveValidationError(
        String field_name,
        String message
) {
}
