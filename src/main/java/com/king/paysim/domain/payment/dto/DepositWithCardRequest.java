package com.king.paysim.domain.payment.dto;

import java.math.BigDecimal;

public record DepositWithCardRequest(
        String linkedCardId,
        BigDecimal amount,
        String email,
        String narration
) {}