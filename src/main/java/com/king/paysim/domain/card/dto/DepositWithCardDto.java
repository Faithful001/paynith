package com.king.paysim.domain.card.dto;

import java.math.BigDecimal;

public record DepositWithCardDto(
    String linkedCardId,
    BigDecimal amount,
    String email,
    String narration
) {
}
