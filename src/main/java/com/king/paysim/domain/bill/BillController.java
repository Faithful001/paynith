package com.king.paysim.domain.bill;

import com.king.paysim.common.response.Response;
import com.king.paysim.domain.payment.dto.CreateBillPaymentDto;
import com.king.paysim.infrastructure.flutterwave.FlutterwaveService;
import com.king.paysim.infrastructure.flutterwave.dto.BillCategoryResult;
import com.king.paysim.infrastructure.flutterwave.dto.GetBillInfoResult;
import com.king.paysim.infrastructure.flutterwave.dto.GetBillerInfoResult;
import com.king.paysim.infrastructure.flutterwave.dto.ValidateCustomerDetailsResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/bills")
public class BillController {
    private final FlutterwaveService flutterwaveService;

    @GetMapping("/bills/categories")
    public ResponseEntity<Response<List<BillCategoryResult.Data>>> getBillCategories() {
        List<BillCategoryResult.Data> result = flutterwaveService.getBillCategories("NG");
        return ResponseEntity.ok(Response.success("Categories retrieved successfully", result));
    }

    @GetMapping("/bills/categories/{categoryCode}/billers")
    public ResponseEntity<Response<List<GetBillerInfoResult.Data>>> getBillerInfo(@PathVariable String categoryCode) {
        List<GetBillerInfoResult.Data> result = flutterwaveService.getBillerInfo(categoryCode, "NG");
        return ResponseEntity.ok(Response.success("Billers retrieved successfully", result));
    }

    @GetMapping("/bills/billers/{billerCode}/items")
    public ResponseEntity<Response<List<GetBillInfoResult.Data>>> getBillInfo(@PathVariable String billerCode) {
        List<GetBillInfoResult.Data> result = flutterwaveService.getBillInfo(billerCode);
        return ResponseEntity.ok(Response.success("Bill items retrieved successfully", result));
    }

    @GetMapping("/bills/items/{itemCode}/validate")
    public ResponseEntity<Response<ValidateCustomerDetailsResult.Data>> validateCustomerDetails(
            @PathVariable String itemCode, @RequestParam String customer) {
        ValidateCustomerDetailsResult.Data result = flutterwaveService.validateCustomerDetails(itemCode, customer);
        return ResponseEntity.ok(Response.success("Customer validated successfully", result));
    }

    @PostMapping("/bills/{billerCode}/items/{itemCode}/payment")
    public ResponseEntity<Response<Object>> createBillPayment(
            @PathVariable String billerCode,
            @PathVariable String itemCode,
            @Valid @RequestBody CreateBillPaymentDto payload
    ) {

        String reference = "paysim_bill_" + UUID.randomUUID() + "_PMCKDU_1";

        Object result = flutterwaveService.createBillPayment(
                billerCode,
                itemCode,
                "NG",
                payload.customer(),
                payload.amount(),
                reference,
                "https://reliable-chipmunk.outray.app/api/v1/webhook/flutterwave/bill-payment"
        );

        return ResponseEntity.ok(Response.success("Bill payment initiated", result));
    }

}
