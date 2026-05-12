package com.king.paysim.domain.wallet;

import com.king.paysim.common.response.Response;
import com.king.paysim.common.util.AuthUtil;
import com.king.paysim.domain.user.entity.User;
import com.king.paysim.domain.wallet.dto.VerifyAccountDto;
import com.king.paysim.domain.wallet.dto.WithdrawalDto;
import com.king.paysim.domain.wallet.dto.WithdrawalResult;
import com.king.paysim.domain.wallet.entity.Wallet;
import com.king.paysim.infrastructure.flutterwave.FlutterwaveService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Wallets", description = "Wallet endpoints")
@RestController
@RequestMapping("/wallets")
@SecurityRequirement(name = "Bearer Auth")
public class WalletController {
    private final WalletService walletService;
    private final AuthUtil authUtil;
    private final FlutterwaveService flutterwaveService;

    public WalletController(WalletService walletService, AuthUtil authUtil, FlutterwaveService flutterwaveService) {
        this.walletService = walletService;
        this.authUtil = authUtil;
        this.flutterwaveService = flutterwaveService;
    }

    @Operation(summary = "Find all user wallets")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Wallets Retrieved"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping
    public ResponseEntity<Response<Wallet>> all() {
        User user = this.authUtil.getAuthUser();

        Wallet wallets = this.walletService.find(user.getId());
        return new ResponseEntity<>(
                Response.success(
                        "Wallets retrieved",
                        wallets
                        ),
                HttpStatus.OK
        );
    }

    @Operation(summary = "Withdraw funds")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Withdrawal initiated"),
            @ApiResponse(responseCode = "404", description = "Wallet not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping("/withdraw")
    public ResponseEntity<Response<WithdrawalResult>> withdraw (
            @Valid @RequestBody WithdrawalDto payload,
            @RequestHeader("Idempotency-Key") String idempotencyKey
    ){
        String userId = this.authUtil.getAuthUserId();

        WithdrawalResult result = this.walletService.withdraw(userId, payload, idempotencyKey);

        return ResponseEntity.ok(Response.success("Withdrawal initiated", result));
    }

    // get list of Nigerian banks
    @GetMapping("/banks")
    public ResponseEntity<Response<?>> getBanks() {
        Object banks = flutterwaveService.getBanks();
        return ResponseEntity.ok(Response.success("Banks retrieved", banks));
    }

    // verify bank account before withdrawal
    @PostMapping("/verify-account")
    public ResponseEntity<Response<?>> verifyAccount(
            @Valid @RequestBody VerifyAccountDto payload
    ) {
        Object result = flutterwaveService.verifyBankAccount(
                payload.accountNumber(),
                payload.bankCode()
        );
        return ResponseEntity.ok(Response.success("Account verified", result));
    }

    @Operation(summary = "Update user by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Wallet retrieved"),
            @ApiResponse(responseCode = "404", description = "Wallet not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Response<Wallet>> findById(@Parameter(description = "wallet id", schema = @Schema(type = "string")) @PathVariable String id) {

        Wallet wallet = this.walletService.findById(id);
        return new ResponseEntity<>(
                Response.success(
                        "Wallets retrieved",
                        wallet
                ),
                HttpStatus.OK
        );
    }

}
