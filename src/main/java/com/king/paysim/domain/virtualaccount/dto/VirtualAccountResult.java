package com.king.paysim.domain.virtualaccount.dto;

public record VirtualAccountResult(
        boolean success,
        String providerRef,      // flw_ref
        String orderRef,         // order_ref
        String accountNumber,
        String bankName,
        String paymentNote,      // note
        String errorMessage
) {}

