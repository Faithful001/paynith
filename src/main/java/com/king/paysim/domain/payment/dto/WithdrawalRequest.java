package com.king.paysim.domain.payment.dto;

import java.math.BigDecimal;

public record WithdrawalRequest(
        BigDecimal amount,
        String bankCode,
        String accountNumber,
        String accountName,
        String narration
) {}