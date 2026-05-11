package com.king.paysim.domain.wallet.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record VerifyAccountDto(
        @NotBlank
        @JsonProperty("account_number")
        String accountNumber,

        @NotBlank
        @JsonProperty("bank_code")
        String bankCode
) {
}
