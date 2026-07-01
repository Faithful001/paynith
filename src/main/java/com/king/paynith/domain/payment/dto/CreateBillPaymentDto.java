package com.king.paynith.domain.payment.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record CreateBillPaymentDto(
        @NotBlank
        BigDecimal amount,

        @NotBlank
        String customer
) {
}
