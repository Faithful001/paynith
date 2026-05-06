package com.king.paysim.domain.virtual_account.dtos;

public record FlutterwaveData(
        String id,
        int amount,
        String account_number,
        String reference,
        String account_bank_name,
        String status,
        String account_expiration_datetime,
        String note,
        String customer_id,
        String created_datetime,
        FlutterwaveMeta meta,
        String customer_reference,
        String currency,
        String narratiom
) {
}
