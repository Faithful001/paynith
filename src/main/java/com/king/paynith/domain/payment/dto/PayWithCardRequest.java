package com.king.paynith.domain.payment.dto;

import java.math.BigDecimal;

public record PayWithCardRequest(
        String cardId,
        BigDecimal amount,
        String email,
        String narration
) {}
