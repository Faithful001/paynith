package com.king.paysim.domain.webhook.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.king.paysim.domain.wallet.dto.Assignment;
import com.king.paysim.domain.wallet.dto.Customer;

public record PaystackSuccessResult(
        String id,

        @JsonProperty("account_number") String accountNumber,
        @JsonProperty("account_name") String accountName,
        @JsonProperty("bank_name") String bankName,
        @JsonProperty("bank_slug") String bankSlug,

        boolean assigned,
        String currency,

        Customer customer,
        Assignment assignment
) {}

