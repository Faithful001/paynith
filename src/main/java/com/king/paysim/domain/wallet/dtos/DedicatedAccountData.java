package com.king.paysim.domain.wallet.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DedicatedAccountData(
        Long id,

        @JsonProperty("account_number") String accountNumber,
        @JsonProperty("account_name") String accountName,
        @JsonProperty("bank_name") String bankName,
        @JsonProperty("bank_slug") String bankSlug,

        boolean assigned,
        String currency,

        Customer customer,
        Assignment assignment
) {}

