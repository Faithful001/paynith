package com.king.paysim.domain.virtual_account.dtos;

public record FlutterwaveValidationError(
        String field_name,
        String message
) {
}
