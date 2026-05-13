package com.king.paysim.domain.bill.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record CreateBillPaymentDto(
        @NotBlank
        String country,

        @NotBlank
        String customerId,

        @NotBlank
        BigDecimal amount,

        @NotBlank
        String reference,

        @NotBlank
        String callbackUrl
) {
}
