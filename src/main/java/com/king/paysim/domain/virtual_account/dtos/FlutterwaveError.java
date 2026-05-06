package com.king.paysim.domain.virtual_account.dtos;

import java.util.List;
import java.util.Optional;

public record FlutterwaveError(
        String type,
        String code,
        String message,
        Optional<List<FlutterwaveValidationError>> validation_errors
) {
}
