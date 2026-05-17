package com.king.paysim.domain.linkedcard.dto;

import java.math.BigDecimal;

public record DirectCardChargeDto(
    String cardNumber,
    String cvv,
    String expiryMonth,
    String expiryYear,
    BigDecimal amount,
    String email,
    String txRef,
    String narration
) {
}
