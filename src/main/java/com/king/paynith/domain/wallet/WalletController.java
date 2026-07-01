package com.king.paynith.domain.wallet;

import com.king.paynith.common.response.Response;
import com.king.paynith.common.util.AuthUtil;
import com.king.paynith.domain.payment.PaymentService;
import com.king.paynith.domain.user.entity.User;
import com.king.paynith.domain.wallet.dto.CreateWalletDto;
import com.king.paynith.domain.wallet.dto.TransactionResult;
import com.king.paynith.domain.wallet.dto.VerifyAccountDto;
import com.king.paynith.domain.wallet.dto.WithdrawalDto;
import com.king.paynith.domain.wallet.entity.Wallet;
import com.king.paynith.infrastructure.flutterwave.FlutterwaveService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Wallets", description = "Wallet management endpoints")
@RestController
@RequestMapping("/wallets")
@SecurityRequirement(name = "Bearer Auth")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;
    private final PaymentService paymentService;
    private final AuthUtil authUtil;
    private final FlutterwaveService flutterwaveService;

    @Operation(summary = "Get user wallet")
    @GetMapping
    public ResponseEntity<Response<Wallet>> getWallet() {
        User user = authUtil.getAuthUser();
        Wallet wallet = walletService.find(user.getId());

        return ResponseEntity.ok(Response.success("Wallet retrieved successfully", wallet));
    }

    @Operation(summary = "Get wallet by ID")
    @GetMapping("/{id}")
    public ResponseEntity<Response<Wallet>> findById(
            @Parameter(description = "Wallet ID") @PathVariable String id) {

        Wallet wallet = walletService.findById(id);
        return ResponseEntity.ok(Response.success("Wallet retrieved", wallet));
    }

    // ====================== WITHDRAWAL ======================
    @Operation(summary = "Withdraw funds from wallet")
    @PostMapping("/withdraw")
    public ResponseEntity<Response<TransactionResult>> withdraw(
            @Valid @RequestBody WithdrawalDto payload,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {

        String userId = authUtil.getAuthUserId();
        TransactionResult result = walletService.withdraw(userId, payload, idempotencyKey);
        return ResponseEntity.ok(Response.success("Withdrawal initiated", result));
    }

    // ====================== BANK UTILITIES ======================

    @Operation(summary = "Get list of Nigerian banks")
    @GetMapping("/banks")
    public ResponseEntity<Response<?>> getBanks() {
        var banks = flutterwaveService.getBanks();
        return ResponseEntity.ok(Response.success("Banks retrieved successfully", banks));
    }

    @Operation(summary = "Verify bank account")
    @PostMapping("/verify-account")
    public ResponseEntity<Response<?>> verifyAccount(@Valid @RequestBody VerifyAccountDto payload) {
        var result = flutterwaveService.verifyBankAccount(payload.accountNumber(), payload.bankCode());
        return ResponseEntity.ok(Response.success("Account verified successfully", result));
    }
}