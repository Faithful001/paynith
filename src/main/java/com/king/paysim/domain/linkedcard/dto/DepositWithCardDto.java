package com.king.paysim.domain.linkedcard.dto;

import java.math.BigDecimal;

public record DepositWithCardDto(
    String linkedCardId,
    BigDecimal amount,
    String email,
    String narration
) {
}
