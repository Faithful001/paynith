package com.king.paysim.domain.wallet.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyAccountDto(
        @NotBlank
        String accountNumber,

        @NotBlank
        @Pattern(regexp = "\\d+", message = "bankCode must be numeric")
        String bankCode
) {
}
