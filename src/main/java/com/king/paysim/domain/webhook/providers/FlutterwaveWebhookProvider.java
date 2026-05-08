package com.king.paysim.domain.webhook.providers;

import com.king.paysim.domain.virtual_account.enums.ProviderName;
import com.king.paysim.domain.wallet.WalletRepository;
import com.king.paysim.domain.webhook.dtos.FlutterwaveChargeCompletedDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;

@Slf4j
@Service
public class FlutterwaveWebhookProvider implements WebhookProvider {

    private final WalletRepository walletRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public FlutterwaveWebhookProvider(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
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

    private void handleChargeCompleted(JsonNode data) {
        try {
            FlutterwaveChargeCompletedDto charge = objectMapper.treeToValue(data, FlutterwaveChargeCompletedDto.class);

            if (!"successful".equalsIgnoreCase(charge.status())) {
                log.warn("Charge not successful: {}", charge.status());
                return;
            }

            // extract userId from tx_ref "paysim_{userId}"
            String txRef = charge.tx_ref();
            if (txRef == null || !txRef.startsWith("paysim_")) {
                log.warn("Unexpected tx_ref format: {}", txRef);
                return;
            }

            String userId = txRef.replace("paysim_", "");

            walletRepository.findByUserId(userId)
                    .ifPresentOrElse(wallet -> {
                        wallet.setBalance(wallet.getBalance().add(BigDecimal.valueOf(charge.amount())));
                        walletRepository.save(wallet);
                        log.info("Wallet funded | UserId={} | Amount={}", userId, charge.amount());
                    }, () -> log.warn("Wallet not found for userId: {}", userId));

        } catch (Exception e) {
            log.error("Failed to process charge.completed", e);
        }
    }

    // Triggered when a virtual account receives a bank transfer
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
    private void handleTransferCompleted(JsonNode data) {
        try {
            FlutterwaveChargeCompletedDto charge = objectMapper.treeToValue(data, FlutterwaveChargeCompletedDto.class);

            if (!"successful".equalsIgnoreCase(charge.status())) {
                log.warn("Charge not successful: {}", charge.status());
                return;
            }

            // extract userId from tx_ref "paysim_{userId}"
            String txRef = charge.tx_ref();
            if (txRef == null || !txRef.startsWith("paysim_")) {
                log.warn("Unexpected tx_ref format: {}", txRef);
                return;
            }

            String userId = txRef.replace("paysim_", "");

            walletRepository.findByUserId(userId)
                    .ifPresentOrElse(wallet -> {
                        wallet.setBalance(wallet.getBalance().add(BigDecimal.valueOf(charge.amount())));
                        walletRepository.save(wallet);
                        log.info("Wallet funded | UserId={} | Amount={}", userId, charge.amount());
                    }, () -> log.warn("Wallet not found for userId: {}", userId));

        } catch (Exception e) {
            log.error("Failed to process charge.completed", e);
        }
    }

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