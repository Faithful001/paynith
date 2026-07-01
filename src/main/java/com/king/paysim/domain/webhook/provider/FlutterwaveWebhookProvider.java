package com.king.paysim.domain.webhook.provider;

import com.king.paysim.domain.transaction.TransactionService;
import com.king.paysim.domain.transaction.dto.CreateTransactionDto;
import com.king.paysim.domain.transaction.enums.TransactionType;
import com.king.paysim.domain.transaction.entity.Transaction;
import com.king.paysim.domain.virtualaccount.enums.ProviderName;
import com.king.paysim.domain.wallet.WalletRepository;
import com.king.paysim.domain.wallet.WalletService;
import com.king.paysim.domain.wallet.entity.Wallet;
import com.king.paysim.domain.webhook.dto.FlutterwaveChargeCompletedResult;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;

@Slf4j
@Service
public class FlutterwaveWebhookProvider implements WebhookProvider {

    private final WalletRepository walletRepository;
    private final TransactionService transactionService;
    private final WalletService walletService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public FlutterwaveWebhookProvider(
            WalletRepository walletRepository,
            TransactionService transactionService,
            WalletService walletService
    ) {
        this.walletRepository = walletRepository;
        this.transactionService = transactionService;
        this.walletService = walletService;
    }

    @Override
    public void handle(String event, JsonNode data) {
        log.info("Flutterwave webhook received: {}", event);

        switch (event) {
            case "charge.completed" -> handleChargeCompleted(data);
            case "transfer.completed" -> handleTransferCompleted(data);
            case "transfer.failed" -> handleTransferFailed(data);
            case "refund.completed" -> handleRefundCompleted(data);
            case "virtualaccount.credited" -> handleVirtualAccountCredited(data);
            default -> log.debug("Unhandled Flutterwave event: {}", event);
        }
    }

    @Override
    public ProviderName getProviderName() {
        return ProviderName.FLUTTERWAVE;
    }

    // ===================== HANDLERS =====================


//    private void creditWallet(
//            Wallet wallet,
//            FlutterwaveChargeCompletedResult charge,
//            String txRef,
//            String userId
//    ) {
//
//        BigDecimal amount = charge.amount();
//
//        // Update wallet balance
//        wallet.setBalance(wallet.getBalance().add(amount));
//        walletRepository.save(wallet);
//
//        // Create transaction record
//        CreateTransactionDto transactionPayload = CreateTransactionDto.builder()
//                .amount(amount)
//                .currency(charge.currency())
//                .walletId(wallet.getId())
//                .transactionType(TransactionType.CREDIT)
//                .providerRef(charge.flw_ref())
//                .reference(txRef)
//                .narration("Wallet funding via bank transfer")
//                .fee(BigDecimal.ZERO)
//                .build();
//
//        transactionService.create(transactionPayload, userId);
//
//        log.info(
//                "Wallet credited successfully | UserId={} | WalletId={} | Amount={} | Ref={}",
//                userId,
//                wallet.getId(),
//                amount,
//                txRef
//        );
//    }

    @Transactional
    public void handleChargeCompleted(JsonNode data) {
        FlutterwaveChargeCompletedResult parsedData = objectMapper
                .treeToValue(data, FlutterwaveChargeCompletedResult.class);

        log.info("Parsed data from charge.completed {}", parsedData);

        if (!"successful".equalsIgnoreCase(parsedData.status())) {
            log.warn("Charge not successful: {}", parsedData.status()); return;
        }
        // extract userId from tx_ref "paysim_{userId}"
        String txRef = parsedData.tx_ref();
        if (txRef == null || !txRef.startsWith("paysim_")) {
            log.warn("Unexpected tx_ref format: {}", txRef); return;
        }

        if (transactionService.existsByReference(txRef)) {
            log.warn("Duplicate webhook ignored | Ref={}", txRef);
            return;
        }

        String[] parts = txRef.split("_");

        String userId = parts[parts.length - 1];

        Wallet wallet = walletRepository.findWalletByUserId(userId)
                .orElseThrow(() -> {
                    log.warn("Wallet not found for userId: {}", userId);
                    return new IllegalStateException("Wallet not found");
                });

        this.walletService.creditWallet(wallet, parsedData, txRef, userId, "Wallet funding via bank transfer");
    }

    // Triggered when a virtual account receives a bank transfer
    @Transactional
    private void handleVirtualAccountCredited(JsonNode data) {
        try {
            String accountNumber = data.path("account_number").asString(null);
            BigDecimal amount = data.path("amount").decimalValue(BigDecimal.valueOf(0));

            if (accountNumber == null) {
                log.warn("No account number in virtualaccount.credited payload");
                return;
            }

            walletRepository.findByAccountNumber(accountNumber)
                    .ifPresentOrElse(wallet -> {
                        Wallet lockedWallet = walletRepository.findWalletById(wallet.getId())
                                .orElseThrow(() -> new IllegalStateException("Wallet not found"));
                        lockedWallet.setBalance(lockedWallet.getBalance().add(amount));
                        walletRepository.save(lockedWallet);
                        log.info("Wallet funded via VA credit | Account={} | Amount={}",
                                accountNumber, amount);
                    }, () -> log.warn("Wallet not found for account: {}", accountNumber));

        } catch (Exception e) {
            log.error("Failed to process virtualaccount.credited", e);
        }
    }

    // Triggered when a transfer/withdrawal is successfully sent out
    @Transactional
    private void handleTransferCompleted(JsonNode data) {
        try {
            String reference = data.path("reference").asString(null);
            if (reference == null) {
                log.warn("Transfer completed webhook missing reference");
                return;
            }

            Transaction transaction = transactionService.getByReference(reference);
            transactionService.markAsSuccessful(transaction.getId());
            log.info("Transfer completed successfully | Reference={}", reference);

        } catch (Exception e) {
            log.error("Failed to process transfer.completed", e);
        }
    }

    @Transactional
    private void handleTransferFailed(JsonNode data) {
        try {
            String reference = data.path("reference").asString(null);
            String reason = data.path("complete_message").asString("Unknown reason");

            log.warn("Transfer failed | Reference={} | Reason={}", reference, reason);

            Transaction transaction = transactionService.getByReference(reference);
            transactionService.markAsFailed(transaction.getId(), reason);

            // Lock and refund the wallet balance
            Wallet wallet = walletRepository.findWalletById(transaction.getWallet().getId())
                    .orElseThrow(() -> new IllegalStateException("Wallet not found"));
            wallet.setBalance(wallet.getBalance().add(transaction.getAmount()));
            walletRepository.save(wallet);

            log.info("Wallet refunded successfully for failed transfer | Reference={} | Amount={}",
                    reference, transaction.getAmount());

        } catch (Exception e) {
            log.error("Failed to process transfer.failed", e);
        }
    }

    @Transactional
    private void handleRefundCompleted(JsonNode data) {
        try {
            String txRef = data.path("tx_ref").asString(null);
            BigDecimal amount = data.path("amount").decimalValue(BigDecimal.valueOf(0));
            String customerEmail = data.path("customer").path("email").asString(null);

            log.info("Refund completed | TxRef={} | Amount={} | Email={}",
                    txRef, amount, customerEmail);

            // credit the wallet back since refund means money is returned
            if (customerEmail != null) {
                walletRepository.findByUserEmail(customerEmail)
                        .ifPresentOrElse(wallet -> {
                            Wallet lockedWallet = walletRepository.findWalletById(wallet.getId())
                                    .orElseThrow(() -> new IllegalStateException("Wallet not found"));
                            lockedWallet.setBalance(lockedWallet.getBalance().add(amount));
                            walletRepository.save(lockedWallet);
                            log.info("Wallet refunded | Email={} | Amount={}", customerEmail, amount);
                        }, () -> log.warn("Wallet not found for refund email: {}", customerEmail));
            }

        } catch (Exception e) {
            log.error("Failed to process refund.completed", e);
        }
    }
}