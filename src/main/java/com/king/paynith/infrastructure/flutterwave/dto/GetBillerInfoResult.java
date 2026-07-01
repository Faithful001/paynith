package com.king.paynith.infrastructure.flutterwave.dto;

import java.util.List;

public record GetBillerInfoResult (
        FlutterwaveApiStatus success,
        String message,
        List<Data> data
) {
    public record Data (
            String id,
            String name,
            String logo,
            String description,
            String short_name,
            String biller_code,
            String country_code
    ){}
}
