package com.king.paysim.domain.wallet.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Customer(
        Long id,
        @JsonProperty("customer_code") String customerCode,
        String email,
        @JsonProperty("first_name") String firstName,
        @JsonProperty("last_name") String lastName
) {}
