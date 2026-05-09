package com.king.paysim.domain.transaction;

import com.king.paysim.common.responses.PaginatedResponse;
import com.king.paysim.common.responses.Response;
import com.king.paysim.common.utils.AuthUtil;
import com.king.paysim.domain.transaction.entities.Transaction;
import com.king.paysim.domain.transaction.enums.TransactionStatus;
import com.king.paysim.domain.transaction.enums.TransactionType;
import com.king.paysim.domain.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final AuthUtil authUtil;

    // ===================== READ =====================

    @GetMapping("/{transactionId}")
    public ResponseEntity<Transaction> getById(
            @PathVariable String transactionId
    ) {
        String userId = authUtil.getAuthUserId();
        return ResponseEntity.ok(
                transactionService.getById(transactionId, userId)
        );
    }

    @GetMapping
    public ResponseEntity<Response<PaginatedResponse<Transaction>>> getAllByUser(
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) String reference,
            @RequestParam(required = false) String providerRef,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        String userId = authUtil.getAuthUserId();

        Page<Transaction> transactions = transactionService.findUserTransactions(
                userId, type, status, reference, providerRef, page, size
        );

        return ResponseEntity.ok(Response.success(
                "Transactions retrieved",
                PaginatedResponse.from(transactions)
        ));
    }

    @GetMapping("/wallet/{walletId}")
    public ResponseEntity<Response<Page<Transaction>>> getAllByWallet(
            @PathVariable String walletId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        String userId = authUtil.getAuthUser().getId();
        return ResponseEntity.ok(Response.success("Transactions retrieved for wallet" + walletId,
                transactionService.getAllByWalletId(walletId, userId, page, size))
        );
    }

    // ===================== UPDATE =====================

    @PatchMapping("/{transactionId}/success")
    public ResponseEntity<Transaction> markAsSuccessful(
            @PathVariable String transactionId
    ) {
        return ResponseEntity.ok(
                transactionService.markAsSuccessful(transactionId)
        );
    }

    @PatchMapping("/{transactionId}/failed")
    public ResponseEntity<Transaction> markAsFailed(
            @PathVariable String transactionId,
            @RequestParam String reason
    ) {
        return ResponseEntity.ok(
                transactionService.markAsFailed(transactionId, reason)
        );
    }

    @PatchMapping("/{transactionId}/reversed")
    public ResponseEntity<Transaction> markAsReversed(
            @PathVariable String transactionId
    ) {
        return ResponseEntity.ok(
                transactionService.markAsReversed(transactionId)
        );
    }

    // ===================== STATS =====================

    @GetMapping("/wallet/{walletId}/total-credited")
    public ResponseEntity<BigDecimal> getTotalCredited(
            @PathVariable String walletId
    ) {
        return ResponseEntity.ok(
                transactionService.getTotalCredited(walletId)
        );
    }

    @GetMapping("/wallet/{walletId}/total-debited")
    public ResponseEntity<BigDecimal> getTotalDebited(
            @PathVariable String walletId
    ) {
        return ResponseEntity.ok(
                transactionService.getTotalDebited(walletId)
        );
    }
}