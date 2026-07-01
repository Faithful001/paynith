package com.king.paynith.infrastructure.flutterwave.dto;

public record FlutterwaveValidationError(
        String field_name,
        String message
) {
}
