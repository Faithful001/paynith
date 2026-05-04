package com.king.paysim.domain.wallet.dtos;

public record Customer(
        long id,
        String first_name,
        String last_name,
        String email,
        String customer_code
) {}
