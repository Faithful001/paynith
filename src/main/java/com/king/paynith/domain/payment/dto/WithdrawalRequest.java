package com.king.paynith.domain.payment.dto;

import java.math.BigDecimal;

public record WithdrawalRequest(
        BigDecimal amount,
        String bankCode,
        String accountNumber,
        String accountName,
        String narration
) {}