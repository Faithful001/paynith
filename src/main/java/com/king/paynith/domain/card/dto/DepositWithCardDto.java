package com.king.paynith.domain.card.dto;

import java.math.BigDecimal;

public record DepositWithCardDto(
    String cardId,
    BigDecimal amount,
    String email,
    String narration
) {
}
