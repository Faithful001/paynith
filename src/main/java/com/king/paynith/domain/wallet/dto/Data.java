package com.king.paynith.domain.wallet.dto;

public record Data(
        Bank bank,
        String account_name,
        String account_number,
        boolean assigned,
        String currency,
        Customer customer,
        Assignment assignment
) {}
