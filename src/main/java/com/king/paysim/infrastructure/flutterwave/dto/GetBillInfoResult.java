package com.king.paysim.infrastructure.flutterwave.dto;

import java.math.BigDecimal;
import java.util.List;

public record GetBillInfoResult(
        FlutterwaveApiStatus status,
        String message,
        List<Data> data
) {
    private record Data (
            Integer id,
            String biller_code,
            String name,
            BigDecimal default_commission,
            String date_added,
            String country,
            Boolean is_airtime,
            String biller_name,
            String item_code,
            String short_name,
            BigDecimal fee,
            Boolean commission_on_fee,
            String reg_expression,
            String label_name,
            BigDecimal amount,
            Boolean is_resolvable,
            String group_name,
            String category_name,
            Boolean is_data,
            BigDecimal default_commission_on_amount,
            Boolean commission_on_fee_or_amount,
            String validity_period
    ){}
}
