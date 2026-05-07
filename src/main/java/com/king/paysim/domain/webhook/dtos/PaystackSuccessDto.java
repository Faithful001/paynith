package com.king.paysim.domain.webhook.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.king.paysim.domain.wallet.dtos.Assignment;
import com.king.paysim.domain.wallet.dtos.Customer;

public record PaystackSuccessDto(
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

