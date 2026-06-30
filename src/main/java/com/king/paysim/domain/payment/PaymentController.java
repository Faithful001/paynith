package com.king.paysim.domain.payment;

import com.king.paysim.common.response.Response;
import com.king.paysim.common.util.AuthUtil;
import com.king.paysim.domain.payment.dto.CreateBillPaymentDto;   // Keep existing DTO
import com.king.paysim.domain.card.CardService;
import com.king.paysim.domain.card.dto.ConfirmCardDto;
import com.king.paysim.domain.card.dto.DirectCardChargeDto;
import com.king.paysim.domain.card.dto.LinkedCardResponse;
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
@RequestMapping("/payments")
@SecurityRequirement(name = "Bearer Auth")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final FlutterwaveService flutterwaveService;
    private final CardService cardService;
    private final AuthUtil authUtil;

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
}