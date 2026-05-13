package com.king.paysim.domain.bill;

import com.king.paysim.common.response.Response;
import com.king.paysim.domain.bill.dto.CreateBillPaymentDto;
import com.king.paysim.infrastructure.flutterwave.FlutterwaveService;
import com.king.paysim.infrastructure.flutterwave.dto.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/bills")
public class BillController {
    private final FlutterwaveService flutterwaveService;

    public BillController(FlutterwaveService flutterwaveService){
        this.flutterwaveService = flutterwaveService;
    }

    @GetMapping("/category")
    public ResponseEntity<Response<BillCategoryResult>> getBillCategories() {
        BillCategoryResult result = flutterwaveService.getBillCategories("NG");

        return ResponseEntity.ok(
                Response.success("Categories retrieved", result)
        );
    }

    @GetMapping("/info")
    public ResponseEntity<Response<GetBillerInfoResult>> getBillerInfo(@PathVariable String categoryCode){
        GetBillerInfoResult result = flutterwaveService.getBillerInfo(categoryCode, "NG");

        return ResponseEntity.ok(
                Response.success("Info retrieved", result)
        );
    }

    @GetMapping("/biller-info")
    public ResponseEntity<Response<GetBillInfoResult>> getBillInfo(@PathVariable String billerCode){
        GetBillInfoResult result = flutterwaveService.getBillInfo(billerCode);

        return ResponseEntity.ok(
                Response.success("Info retrieved", result)
        );
    }

    @GetMapping("/customer/validate")
    public ResponseEntity<Response<ValidateCustomerDetailsResult>> validateCustomerDetails(
            @PathVariable String itemCode,
            @RequestParam String customerId
    ){
        ValidateCustomerDetailsResult result = flutterwaveService.validateCustomerDetails(itemCode, customerId);

        return ResponseEntity.ok(
                Response.success("Info retrieved", result)
        );
    }

    @PostMapping("/payment")
    public ResponseEntity<Response<CreateBillPaymentResult>> validateCustomerDetails(
            @PathVariable String billerCode,
            @PathVariable String itemCode,
            @RequestBody CreateBillPaymentDto payload
    ){
        CreateBillPaymentResult result = flutterwaveService.createBillPayment(
                billerCode,
                itemCode,
                payload.country(),
                payload.customerId(),
                payload.amount(),
                payload.reference(),
                payload.reference()
                );

        return ResponseEntity.ok(
                Response.success("Info retrieved", result)
        );
    }



}
