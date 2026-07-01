package com.king.paynith.domain.transaction;

import com.king.paynith.domain.transaction.entity.Transaction;
import com.king.paynith.domain.transaction.enums.TransactionStatus;
import com.king.paynith.domain.transaction.enums.TransactionType;
import com.king.paynith.domain.wallet.WalletRepository;
import com.king.paynith.domain.wallet.entity.Wallet;
import com.king.paynith.infrastructure.flutterwave.FlutterwaveService;
import com.king.paynith.infrastructure.flutterwave.dto.FlwTransactionResponse;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
public class ReconciliationScheduler {

    private final TransactionRepository transactionRepository;
    private final TransactionService transactionService;
    private final WalletRepository walletRepository;
    private final FlutterwaveService flutterwaveService;

    public ReconciliationScheduler(
            TransactionRepository transactionRepository,
            TransactionService transactionService,
            WalletRepository walletRepository,
            FlutterwaveService flutterwaveService
    ) {
        this.transactionRepository = transactionRepository;
        this.transactionService = transactionService;
        this.walletRepository = walletRepository;
        this.flutterwaveService = flutterwaveService;
    }

    /**
     * Reconcile pending transactions older than 10 minutes every 5 minutes.
     */
    @Scheduled(fixedDelay = 300000) // 5 minutes
    public void reconcilePendingTransactions() {
        log.info("Starting background transaction reconciliation...");

        LocalDateTime thresholdTime = LocalDateTime.now().minusMinutes(10);
        List<Transaction> pendingTransactions = transactionRepository
                .findByStatusAndCreatedAtBefore(TransactionStatus.PENDING, thresholdTime);

        if (pendingTransactions.isEmpty()) {
            log.info("No pending transactions found for reconciliation.");
            return;
        }

        log.info("Found {} pending transactions to reconcile.", pendingTransactions.size());

        for (Transaction tx : pendingTransactions) {
            try {
                if (tx.getType() == TransactionType.WITHDRAWAL) {
                    reconcileWithdrawal(tx);
                } else {
                    reconcileDepositOrPayment(tx);
                }
            } catch (Exception e) {
                log.error("Error reconciling transaction ID={}", tx.getId(), e);
            }
        }

        log.info("Transaction reconciliation cycle completed.");
    }

    @Transactional
    private void reconcileWithdrawal(Transaction tx) {
        log.info("Reconciling withdrawal transaction ID={} | Ref={}", tx.getId(), tx.getReference());

        JsonNode response = flutterwaveService.verifyTransfer(tx.getReference());
        if (response == null || !"success".equalsIgnoreCase(response.path("status").asString(null))) {
            log.warn("Could not retrieve transfer status from provider for reference={}", tx.getReference());
            return;
        }

        JsonNode dataNode = response.path("data");

        if (dataNode.isArray() && !dataNode.isEmpty()) {
            dataNode = dataNode.get(0);
        }

        String flwStatus = dataNode.path("status").asString(null);
        if (flwStatus == null) {
            log.warn("Transfer status is empty in provider response for reference={}", tx.getReference());
            return;
        }

        log.info("Provider transfer status for ref={}: {}", tx.getReference(), flwStatus);

        if ("SUCCESSFUL".equalsIgnoreCase(flwStatus)) {
            transactionService.markAsSuccessful(tx.getId());
            log.info("Successfully reconciled withdrawal to SUCCESSFUL | Ref={}", tx.getReference());

        } else if ("FAILED".equalsIgnoreCase(flwStatus)) {
            String reason = dataNode.path("complete_message").asString("Transfer failed according to provider reconciliation");
            
            // Mark transaction as failed
            transactionService.markAsFailed(tx.getId(), reason);

            // Lock and refund wallet balance (since withdrawal debited wallet immediately)
            Wallet wallet = walletRepository.findWalletById(tx.getWallet().getId())
                    .orElseThrow(() -> new IllegalStateException("Wallet not found"));
            wallet.setBalance(wallet.getBalance().add(tx.getAmount()));
            walletRepository.save(wallet);

            log.info("Successfully reconciled withdrawal to FAILED and refunded wallet | Ref={} | Amount={}",
                    tx.getReference(), tx.getAmount());
        }
    }

    @Transactional
    private void reconcileDepositOrPayment(Transaction tx) {
        log.info("Reconciling deposit/payment transaction ID={} | Ref={}", tx.getId(), tx.getReference());

        FlwTransactionResponse response = flutterwaveService.verifyTransaction(tx.getReference());
        if (response == null || !"success".equalsIgnoreCase(response.status()) || response.data() == null) {
            log.warn("Could not verify transaction status from provider for reference={}", tx.getReference());
            return;
        }

        String flwStatus = response.data().status();
        log.info("Provider transaction status for ref={}: {}", tx.getReference(), flwStatus);

        if ("successful".equalsIgnoreCase(flwStatus)) {
            // Reconcile and transition transaction status to SUCCESSFUL in DB
            transactionService.markAsSuccessful(tx.getId());

            // If it was a CREDIT (deposit) transaction, make sure wallet has been credited
            if (tx.getType() == TransactionType.CREDIT) {
                Wallet wallet = walletRepository.findWalletById(tx.getWallet().getId())
                        .orElseThrow(() -> new IllegalStateException("Wallet not found"));
                
                // Fetch the total amount credited to prevent duplicate credit logic
                // If it is already marked as SUCCESSFUL, do not credit again.
                // Note: since this transaction was PENDING, the wallet was NOT credited.
                // So now we credit it.
                wallet.setBalance(wallet.getBalance().add(tx.getAmount()));
                walletRepository.save(wallet);
                log.info("Credited wallet for reconciled credit transaction | Ref={} | Amount={}",
                        tx.getReference(), tx.getAmount());
            }
            log.info("Successfully reconciled deposit/payment to SUCCESSFUL | Ref={}", tx.getReference());

        } else if ("failed".equalsIgnoreCase(flwStatus)) {
            transactionService.markAsFailed(tx.getId(), "Transaction failed according to provider reconciliation");
            log.info("Successfully reconciled deposit/payment to FAILED | Ref={}", tx.getReference());
        }
    }
}
