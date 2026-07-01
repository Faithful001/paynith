package com.king.paynith.domain.transaction;

import com.king.paynith.common.response.PaginatedResponse;
import com.king.paynith.common.response.Response;
import com.king.paynith.common.util.AuthUtil;
import com.king.paynith.domain.transaction.entity.Transaction;
import com.king.paynith.domain.transaction.enums.TransactionStatus;
import com.king.paynith.domain.transaction.enums.TransactionType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Tag(name = "Transaction", description = "Transaction endpoints")
@SecurityRequirement(name = "Bearer Auth")
@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {
    private final TransactionService transactionService;
    private final AuthUtil authUtil;

    // ===================== READ =====================

    @Operation(summary = "Get a transaction by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transaction retrieved"),
            @ApiResponse(responseCode = "404", description = "Transaction not found"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/{transactionId}")
    public ResponseEntity<Response<Transaction>> getById(
            @Parameter(description = "Transaction ID", required = true)
            @PathVariable String transactionId
    ) {
        String userId = authUtil.getAuthUserId();
        return ResponseEntity.ok(
                Response.success("Transaction retrieved", transactionService.getById(transactionId, userId))
        );
    }

    @Operation(summary = "Get all transactions for authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transactions retrieved"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping
    public ResponseEntity<Response<PaginatedResponse<Transaction>>> getAllByUser(
            @Parameter(description = "Filter by transaction type") @RequestParam(required = false) TransactionType type,
            @Parameter(description = "Filter by transaction status") @RequestParam(required = false) TransactionStatus status,
            @Parameter(description = "Filter by internal reference") @RequestParam(required = false) String reference,
            @Parameter(description = "Filter by provider reference") @RequestParam(required = false) String providerRef,
            @Parameter(description = "Page number (0-based)", schema = @Schema(type = "integer", defaultValue = "0")) @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", schema = @Schema(type = "integer", defaultValue = "20")) @RequestParam(defaultValue = "20") int size
    ) {
        String userId = authUtil.getAuthUserId();
        Page<Transaction> transactions = transactionService.findUserTransactions(
                userId, type, status, reference, providerRef, page, size
        );
        return ResponseEntity.ok(Response.success("Transactions retrieved", PaginatedResponse.from(transactions)));
    }

    @Operation(summary = "Get all transactions for a wallet")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transactions retrieved"),
            @ApiResponse(responseCode = "404", description = "Wallet not found"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/wallet/{walletId}")
    public ResponseEntity<Response<PaginatedResponse<Transaction>>> getAllByWallet(
            @Parameter(description = "Wallet ID", required = true) @PathVariable String walletId,
            @Parameter(description = "Page number (0-based)", schema = @Schema(type = "integer", defaultValue = "0")) @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", schema = @Schema(type = "integer", defaultValue = "20")) @RequestParam(defaultValue = "20") int size
    ) {
        String userId = authUtil.getAuthUser().getId();
        return ResponseEntity.ok(Response.success(
                "Transactions retrieved for wallet " + walletId,
                PaginatedResponse.from(transactionService.getAllByWalletId(walletId, userId, page, size))
        ));
    }

    // ===================== UPDATE =====================

    @Operation(summary = "Mark a transaction as successful")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transaction marked as successful"),
            @ApiResponse(responseCode = "404", description = "Transaction not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PatchMapping("/{transactionId}/success")
    public ResponseEntity<Response<Transaction>> markAsSuccessful(
            @Parameter(description = "Transaction ID", required = true) @PathVariable String transactionId
    ) {
        return ResponseEntity.ok(
                Response.success("Transaction marked as successful", transactionService.markAsSuccessful(transactionId))
        );
    }

    @Operation(summary = "Mark a transaction as failed")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transaction marked as failed"),
            @ApiResponse(responseCode = "404", description = "Transaction not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PatchMapping("/{transactionId}/failed")
    public ResponseEntity<Response<Transaction>> markAsFailed(
            @Parameter(description = "Transaction ID", required = true) @PathVariable String transactionId,
            @Parameter(description = "Reason for failure", required = true) @RequestParam String reason
    ) {
        return ResponseEntity.ok(
                Response.success("Transaction marked as failed", transactionService.markAsFailed(transactionId, reason))
        );
    }

    @Operation(summary = "Mark a transaction as reversed")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transaction marked as reversed"),
            @ApiResponse(responseCode = "404", description = "Transaction not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PatchMapping("/{transactionId}/reversed")
    public ResponseEntity<Response<Transaction>> markAsReversed(
            @Parameter(description = "Transaction ID", required = true) @PathVariable String transactionId
    ) {
        return ResponseEntity.ok(
                Response.success("Transaction marked as reversed", transactionService.markAsReversed(transactionId))
        );
    }

    // ===================== STATS =====================

    @Operation(summary = "Get total amount credited to a wallet")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Total credited amount retrieved"),
            @ApiResponse(responseCode = "404", description = "Wallet not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/wallet/{walletId}/total-credited")
    public ResponseEntity<Response<BigDecimal>> getTotalCredited(
            @Parameter(description = "Wallet ID", required = true) @PathVariable String walletId
    ) {
        return ResponseEntity.ok(
                Response.success("Total credited retrieved", transactionService.getTotalCredited(walletId))
        );
    }

    @Operation(summary = "Get total amount debited from a wallet")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Total debited amount retrieved"),
            @ApiResponse(responseCode = "404", description = "Wallet not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/wallet/{walletId}/total-debited")
    public ResponseEntity<Response<BigDecimal>> getTotalDebited(
            @Parameter(description = "Wallet ID", required = true) @PathVariable String walletId
    ) {
        return ResponseEntity.ok(
                Response.success("Total debited retrieved", transactionService.getTotalDebited(walletId))
        );
    }
}