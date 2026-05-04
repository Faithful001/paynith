package com.king.paysim.domain.wallet.dtos;

public record Assignment(
        long integration,
        long assignee_id,
        String assignee_type,
        boolean expired,
        String account_type,
        String assigned_at
) {}
