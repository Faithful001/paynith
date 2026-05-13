package com.king.paysim.infrastructure.flutterwave.dto;

import java.util.List;

public record GetBanksResult(
        FlutterwaveApiStatus status,
        String message,
        List<Data> data
) {
    private record Data (
            Integer id,
            String code,
            String name
    ){}
}
