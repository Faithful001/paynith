package com.king.paysim.domain.virtual_account.dtos;

public record VirtualAccountResult(
        boolean success,
        String providerRef,
        String accountNumber,
        String bankName,
        String bankSlug,
        String errorMessage
) {}

