package com.king.paysim.domain.bill;

import com.king.paysim.common.response.Response;
import com.king.paysim.common.util.AuthUtil;
import com.king.paysim.domain.bill.dto.CreateBillPaymentDto;
import com.king.paysim.infrastructure.flutterwave.FlutterwaveService;
import com.king.paysim.infrastructure.flutterwave.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Bill", description = "Bill endpoints")
@SecurityRequirement(name = "Bearer Auth")
@RestController
@RequestMapping("/bills")
public class BillController {

    private final FlutterwaveService flutterwaveService;

    public BillController(FlutterwaveService flutterwaveService, AuthUtil authUtil) {
        this.flutterwaveService = flutterwaveService;
    }

    // 1. Get all bill categories
    @Operation(summary = "Get bill categories")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Categories retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Flutterwave error"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/categories")
    public ResponseEntity<Response<BillCategoryResult>> getBillCategories() {
        BillCategoryResult result = flutterwaveService.getBillCategories("NG");

        return ResponseEntity.ok(
                Response.success("Categories retrieved successfully", result)
        );
    }

    // 2. Get billers under a category (e.g. /bills/categories/DONATIONS/billers)
    @Operation(summary = "Get billers")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Billers retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Flutterwave error"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/categories/{categoryCode}/billers")
    public ResponseEntity<Response<GetBillerInfoResult>> getBillerInfo(
            @Parameter(
                    description = "Category code",
                    required = true
//                    schema = @Schema(type = "integer")
            )
            @PathVariable
            String categoryCode) {

        GetBillerInfoResult result = flutterwaveService.getBillerInfo(categoryCode, "NG");

        return ResponseEntity.ok(
                Response.success("Billers retrieved successfully", result)
        );
    }

    // 3. Get bill items/packages for a specific biller
    @Operation(summary = "Get bill items/packages for a specific biller")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Bill items retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Flutterwave error"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/billers/{billerCode}/items")
    public ResponseEntity<Response<GetBillInfoResult>> getBillInfo(
            @Parameter(
                    description = "Biller code",
                    required = true
            )
            @PathVariable String billerCode) {

        GetBillInfoResult result = flutterwaveService.getBillInfo(billerCode);

        return ResponseEntity.ok(
                Response.success("Bill items retrieved successfully", result)
        );
    }

    // 4. Validate customer (e.g. meter number, smartcard number, etc.)
    @Operation(summary = "Validate customer")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Customer validated successfully"),
            @ApiResponse(responseCode = "500", description = "Flutterwave error"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/items/{itemCode}/validate")
    public ResponseEntity<Response<ValidateCustomerDetailsResult>> validateCustomerDetails(
            @Parameter(
                    description = "Item code",
                    required = true
//                    schema = @Schema(type = "integer")
            )
            @PathVariable String itemCode,
            @Parameter(
                    description = "Customer phone number/IUC number/meter no",
                    required = true
            )
            @RequestParam String customer) {

        ValidateCustomerDetailsResult result = flutterwaveService.validateCustomerDetails(itemCode, customer);

        return ResponseEntity.ok(
                Response.success("Customer validated successfully", result)
        );
    }

    // 5. Create bill payment
    @PostMapping("/{billerCode}/items/{itemCode}/payment")
    public ResponseEntity<Response<CreateBillPaymentResult>> createBillPayment(
            @Parameter(
                    description = "Biller code",
                    required = true
            )
            @PathVariable String billerCode,
            @Parameter(
                    description = "Item code",
                    required = true
            )
            @PathVariable String itemCode,
            @Valid @RequestBody CreateBillPaymentDto payload
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