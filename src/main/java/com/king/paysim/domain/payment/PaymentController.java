package com.king.paysim.domain.payment;

import com.king.paysim.common.response.Response;
import com.king.paysim.common.util.AuthUtil;
import com.king.paysim.domain.payment.dto.CreateBillPaymentDto;   // Keep existing DTO
import com.king.paysim.domain.linkedcard.LinkedCardService;
import com.king.paysim.domain.linkedcard.dto.ConfirmCardDto;
import com.king.paysim.domain.linkedcard.dto.DirectCardChargeDto;
import com.king.paysim.domain.linkedcard.dto.LinkedCardResponse;
import com.king.paysim.domain.payment.dto.*;
import com.king.paysim.domain.wallet.dto.TransactionResult;
import com.king.paysim.domain.wallet.dto.WithdrawalDto;
import com.king.paysim.infrastructure.flutterwave.FlutterwaveService;
import com.king.paysim.infrastructure.flutterwave.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Payments", description = "All payment operations (Wallet, Card, Bills, Transfers)")
@RestController
@RequestMapping("/api/v1/payments")
@SecurityRequirement(name = "Bearer Auth")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final FlutterwaveService flutterwaveService;
    private final LinkedCardService linkedCardService;
    private final AuthUtil authUtil;

    // ====================== CARD LINKING ======================
    @PostMapping("/cards/direct-charge")
    public ResponseEntity<Response<Object>> directCardCharge(@Valid @RequestBody DirectCardChargeDto request) {
        String userId = authUtil.getAuthUserId();
        Object response = paymentService.initiateCardLinking(request, userId);
        return ResponseEntity.ok(Response.success("Card charge initiated", response));
    }

    @PostMapping("/cards/confirm")
    public ResponseEntity<Response<LinkedCardResponse>> confirmCardLinking(@Valid @RequestBody ConfirmCardDto request) {
        var user = authUtil.getAuthUser();
        var linkedCard = paymentService.confirmCardLinking(request.txRef(), user);
        return ResponseEntity.ok(Response.success("Card linked successfully", LinkedCardResponse.fromEntity(linkedCard)));
    }

    @GetMapping("/cards")
    public ResponseEntity<Response<List<LinkedCardResponse>>> getLinkedCards() {
        String userId = authUtil.getAuthUserId();
        List<LinkedCardResponse> cards = linkedCardService.getUserLinkedCards(userId);
        return ResponseEntity.ok(Response.success("Linked cards retrieved", cards));
    }

    // ====================== PAYMENTS ======================

    @PostMapping("/pay/wallet")
    public ResponseEntity<Response<TransactionResult>> payWithWallet(@Valid @RequestBody PayWithWalletRequest request) {
        String userId = authUtil.getAuthUserId();
        TransactionResult result = paymentService.payWithWalletBalance(request, userId);
        return ResponseEntity.ok(Response.success("Payment successful", result));
    }

    @PostMapping("/deposit/card")
    public ResponseEntity<Response<TransactionResult>> fundWalletWithCard(@Valid @RequestBody DepositWithCardRequest request) {
        String userId = authUtil.getAuthUserId();
        TransactionResult result = paymentService.fundWalletWithLinkedCard(request, userId);
        return ResponseEntity.ok(Response.success("Wallet funded successfully", result));
    }

    @PostMapping("/pay/card")
    public ResponseEntity<Response<TransactionResult>> payWithCard(@Valid @RequestBody PayWithCardRequest request) {
        String userId = authUtil.getAuthUserId();
        TransactionResult result = paymentService.payWithLinkedCard(request, userId);
        return ResponseEntity.ok(Response.success("Payment successful", result));
    }

    // ====================== BILL PAYMENTS ======================

    @GetMapping("/bills/categories")
    public ResponseEntity<Response<BillCategoryResult>> getBillCategories() {
        BillCategoryResult result = flutterwaveService.getBillCategories("NG");
        return ResponseEntity.ok(Response.success("Categories retrieved successfully", result));
    }

    @GetMapping("/bills/categories/{categoryCode}/billers")
    public ResponseEntity<Response<GetBillerInfoResult>> getBillerInfo(@PathVariable String categoryCode) {
        GetBillerInfoResult result = flutterwaveService.getBillerInfo(categoryCode, "NG");
        return ResponseEntity.ok(Response.success("Billers retrieved successfully", result));
    }

    @GetMapping("/bills/billers/{billerCode}/items")
    public ResponseEntity<Response<GetBillInfoResult>> getBillInfo(@PathVariable String billerCode) {
        GetBillInfoResult result = flutterwaveService.getBillInfo(billerCode);
        return ResponseEntity.ok(Response.success("Bill items retrieved successfully", result));
    }

    @GetMapping("/bills/items/{itemCode}/validate")
    public ResponseEntity<Response<ValidateCustomerDetailsResult>> validateCustomerDetails(
            @PathVariable String itemCode, @RequestParam String customer) {
        ValidateCustomerDetailsResult result = flutterwaveService.validateCustomerDetails(itemCode, customer);
        return ResponseEntity.ok(Response.success("Customer validated successfully", result));
    }

    @PostMapping("/bills/{billerCode}/items/{itemCode}/payment")
    public ResponseEntity<Response<CreateBillPaymentResult>> createBillPayment(
            @PathVariable String billerCode,
            @PathVariable String itemCode,
            @Valid @RequestBody CreateBillPaymentDto payload
    ) {

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

        return ResponseEntity.ok(Response.success("Bill payment initiated", result));
    }

    // ====================== WITHDRAWAL ======================
    @Operation(summary = "Withdraw funds from wallet")
    @PostMapping("/withdraw")
    public ResponseEntity<Response<TransactionResult>> withdraw(
            @Valid @RequestBody WithdrawalDto payload,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {

        String userId = authUtil.getAuthUserId();
        TransactionResult result = paymentService.withdraw(userId, payload, idempotencyKey);
        return ResponseEntity.ok(Response.success("Withdrawal initiated", result));
    }
}