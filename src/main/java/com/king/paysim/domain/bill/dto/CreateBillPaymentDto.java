package com.king.paysim.domain.bill.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record CreateBillPaymentDto(
        @NotBlank
        BigDecimal amount,

        @NotBlank
        String customer
) {
}
