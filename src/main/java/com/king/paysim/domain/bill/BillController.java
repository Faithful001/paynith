package com.king.paysim.domain.bill;

import com.king.paysim.common.response.Response;
import com.king.paysim.common.util.AuthUtil;
import com.king.paysim.domain.bill.dto.CreateBillPaymentDto;
import com.king.paysim.infrastructure.flutterwave.FlutterwaveService;
import com.king.paysim.infrastructure.flutterwave.dto.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/bills")
public class BillController {

    private final FlutterwaveService flutterwaveService;
    private final AuthUtil authUtil;

    public BillController(FlutterwaveService flutterwaveService, AuthUtil authUtil) {
        this.flutterwaveService = flutterwaveService;
        this.authUtil = authUtil;
    }

    // 1. Get all bill categories
    @GetMapping("/categories")
    public ResponseEntity<Response<BillCategoryResult>> getBillCategories() {
        BillCategoryResult result = flutterwaveService.getBillCategories("NG");

        return ResponseEntity.ok(
                Response.success("Categories retrieved successfully", result)
        );
    }

    // 2. Get billers under a category (e.g. /bills/categories/DONATIONS/billers)
    @GetMapping("/categories/{categoryCode}/billers")
    public ResponseEntity<Response<GetBillerInfoResult>> getBillerInfo(
            @PathVariable String categoryCode) {

        GetBillerInfoResult result = flutterwaveService.getBillerInfo(categoryCode, "NG");

        return ResponseEntity.ok(
                Response.success("Billers retrieved successfully", result)
        );
    }

    // 3. Get bill items/packages for a specific biller
    @GetMapping("/billers/{billerCode}/items")
    public ResponseEntity<Response<GetBillInfoResult>> getBillInfo(
            @PathVariable String billerCode) {

        GetBillInfoResult result = flutterwaveService.getBillInfo(billerCode);

        return ResponseEntity.ok(
                Response.success("Bill items retrieved successfully", result)
        );
    }

    // 4. Validate customer (e.g. meter number, smartcard number, etc.)
    @GetMapping("/items/{itemCode}/validate")
    public ResponseEntity<Response<ValidateCustomerDetailsResult>> validateCustomerDetails(
            @PathVariable String itemCode,
            @RequestParam String customer) {   // renamed to 'customer' for clarity

        ValidateCustomerDetailsResult result = flutterwaveService.validateCustomerDetails(itemCode, customer);

        return ResponseEntity.ok(
                Response.success("Customer validated successfully", result)
        );
    }

    // 5. Create bill payment
    @PostMapping("/{billerCode}/items/{itemCode}/payment")
    public ResponseEntity<Response<CreateBillPaymentResult>> createBillPayment(
            @PathVariable String billerCode,
            @PathVariable String itemCode,
            @RequestBody CreateBillPaymentDto payload
    ){
        String reference = "paysim_bill_" + UUID.randomUUID() + "_PMCKDU_1";

        CreateBillPaymentResult result = flutterwaveService.createBillPayment(
                billerCode,
                itemCode,
                "NG",
                payload.customer(),
                payload.amount(),
                reference,
                "https://reliable-chipmunk.outray.app/api/v1/webhook/flutterwave/bill-payment"
        );

        return ResponseEntity.ok(
                Response.success("Bill initiated", result)
        );
    }
}