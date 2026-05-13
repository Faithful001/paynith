package com.king.paysim.infrastructure.flutterwave.dto;

import java.util.List;

public record BillCategoryResult(
        FlutterwaveApiStatus status,
        String message,
        List<Data> data

) {
    private  record Data(
            Integer id,
            String name,
            String code,
            String description,
            String country_code
    ){}
}
