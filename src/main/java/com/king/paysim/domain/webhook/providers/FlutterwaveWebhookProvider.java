package com.king.paysim.domain.webhook.providers;

import com.king.paysim.domain.transaction.TransactionService;
import com.king.paysim.domain.transaction.dtos.CreateTransactionDto;
import com.king.paysim.domain.transaction.entities.Transaction;
import com.king.paysim.domain.transaction.enums.TransactionType;
import com.king.paysim.domain.virtual_account.enums.ProviderName;
import com.king.paysim.domain.wallet.WalletRepository;
import com.king.paysim.domain.wallet.entities.Wallet;
import com.king.paysim.domain.webhook.dtos.FlutterwaveChargeCompletedDto;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.Optional;

@Slf4j
@Service
public class FlutterwaveWebhookProvider implements WebhookProvider {

    private final WalletRepository walletRepository;
    private final TransactionService transactionService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public FlutterwaveWebhookProvider(WalletRepository walletRepository, TransactionService transactionService) {
        this.walletRepository = walletRepository;
        this.transactionService = transactionService;
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


    private void creditWallet(
            Wallet wallet,
            FlutterwaveChargeCompletedDto charge,
            String txRef,
            String userId
    ) {

        BigDecimal amount = BigDecimal.valueOf(charge.amount());

        // Update wallet balance
        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepository.save(wallet);

        // Create transaction record
        CreateTransactionDto transactionPayload = CreateTransactionDto.builder()
                .amount(amount)
                .currency(charge.currency())
                .walletId(wallet.getId())
                .transactionType(TransactionType.CREDIT)
                .providerRef(Optional.ofNullable(charge.tx_ref()))
                .reference(Optional.ofNullable(txRef))
                .build();

        transactionService.create(transactionPayload, userId);

        log.info(
                "Wallet credited successfully | UserId={} | WalletId={} | Amount={} | Ref={}",
                userId,
                wallet.getId(),
                amount,
                txRef
        );
    }

    @Transactional
    public void handleChargeCompleted(JsonNode data) {
        FlutterwaveChargeCompletedDto charge = objectMapper
                .treeToValue(data, FlutterwaveChargeCompletedDto.class);

        if (!"successful".equalsIgnoreCase(charge.status())) {
            log.warn("Charge not successful: {}", charge.status()); return;
        }
        // extract userId from tx_ref "paysim_{userId}"
        String txRef = charge.tx_ref();
        if (txRef == null || !txRef.startsWith("paysim_")) {
            log.warn("Unexpected tx_ref format: {}", txRef); return;
        }

        if (transactionService.existsByReference(txRef)) {
            log.warn("Duplicate webhook ignored | Ref={}", txRef);
            return;
        }

        String userId = txRef.replace("paysim_", "");

        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> {
                    log.warn("Wallet not found for userId: {}", userId);
                    return new IllegalStateException("Wallet not found");
                });

        creditWallet(wallet, charge, txRef, userId);
    }

    // Triggered when a virtual account receives a bank transfer
    @Transactional
    private void handleVirtualAccountCredited(JsonNode data) {
        try {
            String accountNumber = data.path("account_number").asString(null);
            long amount = data.path("amount").asLong(0);

            if (accountNumber == null) {
                log.warn("No account number in virtualaccount.credited payload");
                return;
            }

            walletRepository.findByAccountNumber(accountNumber)
                    .ifPresentOrElse(wallet -> {
                        wallet.setBalance(wallet.getBalance().add(BigDecimal.valueOf(amount)));
                        walletRepository.save(wallet);
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
            FlutterwaveChargeCompletedDto charge = objectMapper
                    .treeToValue(data, FlutterwaveChargeCompletedDto.class);

            if (!"successful".equalsIgnoreCase(charge.status())) {
                log.warn("Charge not successful: {}", charge.status()); return;
            }
            // extract userId from tx_ref "paysim_{userId}"
            String txRef = charge.tx_ref();
            if (txRef == null || !txRef.startsWith("paysim_")) {
                log.warn("Unexpected tx_ref format: {}", txRef); return;
            }

            if (transactionService.existsByReference(txRef)) {
                log.warn("Duplicate webhook ignored | Ref={}", txRef);
                return;
            }

            String userId = txRef.replace("paysim_", "");

            Wallet wallet = walletRepository.findByUserId(userId)
                    .orElseThrow(() -> {
                        log.warn("Wallet not found for userId: {}", userId);
                        return new IllegalStateException("Wallet not found");
                    });

            creditWallet(wallet, charge, txRef, userId);

        } catch (Exception e) {
            log.error("Failed to process charge.completed", e);
        }
    }

    @Transactional
    private void handleTransferFailed(JsonNode data) {
        try {
            String reference = data.path("reference").asString(null);
            String reason = data.path("complete_message").asString("Unknown reason");

            log.warn("Transfer failed | Reference={} | Reason={}", reference, reason);

            // TODO: update transaction record status to FAILED
            // and refund the wallet balance

        } catch (Exception e) {
            log.error("Failed to process transfer.failed", e);
        }
    }

    @Transactional
    private void handleRefundCompleted(JsonNode data) {
        try {
            String txRef = data.path("tx_ref").asString(null);
            long amount = data.path("amount").asLong(0);
            String customerEmail = data.path("customer").path("email").asString(null);

            log.info("Refund completed | TxRef={} | Amount={} | Email={}",
                    txRef, amount, customerEmail);

            // credit the wallet back since refund means money is returned
            if (customerEmail != null) {
                walletRepository.findByUserEmail(customerEmail)
                        .ifPresentOrElse(wallet -> {
                            wallet.setBalance(wallet.getBalance().add(BigDecimal.valueOf(amount)));
                            walletRepository.save(wallet);
                            log.info("Wallet refunded | Email={} | Amount={}", customerEmail, amount);
                        }, () -> log.warn("Wallet not found for refund email: {}", customerEmail));
            }

        } catch (Exception e) {
            log.error("Failed to process refund.completed", e);
        }
    }
}