package com.king.paynith.domain.payment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record PayWithWalletRequest(

        @NotNull
        @Positive
        BigDecimal amount,

        String narration,

        String reference,

        String metadata
) {}