package com.king.paysim.domain.webhook.provider;

import com.king.paysim.domain.virtualaccount.enums.ProviderName;
import com.king.paysim.domain.wallet.WalletRepository;
import com.king.paysim.domain.webhook.dto.PaystackSuccessDto;
import com.king.paysim.domain.wallet.entity.Wallet;
import com.king.paysim.domain.wallet.enums.WalletStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

@Slf4j
@Service
public class PaystackWebhookProvider implements WebhookProvider {

    private final WalletRepository walletRepository;

    public PaystackWebhookProvider(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    @Override
    public void handle(String event, JsonNode data) {
        log.info("Processing Paystack webhook: {}", event);
        try {
            switch (event) {
                case "dedicatedaccount.assign.success":
                    PaystackSuccessDto dedicatedData = convertToDedicatedAccountData(data);
                    handleAssignSuccess(dedicatedData);
                    break;

                case "dedicatedaccount.assign.failed":
                    handleAssignFailed(data);
                    break;

                case "customeridentification.success":
                case "customeridentification.failed":
                    log.info("Customer identification event received: {}", event);
                    break;

                case "charge.success":
                    handleChargeSuccess(data);
                    break;

                default:
                    log.debug("Unhandled webhook event: {}", event);
            }
        } catch (Exception e) {
            log.error("Error processing webhook event: {}", event, e);
        }
    }

    private PaystackSuccessDto convertToDedicatedAccountData(JsonNode node) {
        try {
            return new ObjectMapper().treeToValue(node, PaystackSuccessDto.class);
        } catch (Exception e) {
            log.error("Failed to convert JsonNode to DedicatedAccountData", e);
            throw new RuntimeException("Failed to parse dedicated account data", e);
        }
    }

    @Override
    public ProviderName getProviderName() {
        return ProviderName.PAYSTACK;
    }

    private void handleAssignSuccess(PaystackSuccessDto data) {
        if (data.customer() == null) {
            log.warn("No customer data in dedicatedaccount.assign.success");
            return;
        }

        Optional<Wallet> optionalWallet = walletRepository.findByUserEmail(data.customer().email());

        if (optionalWallet.isPresent()) {
            Wallet wallet = optionalWallet.get();
            wallet.setStatus(WalletStatus.ACTIVE);
            wallet.setAccountNumber(data.accountNumber());
            wallet.setBankName(data.bankName());
//            wallet.setBankSlug(data.bankSlug());
//            wallet.setDedicatedAccountId(data.id());
//            wallet.setCustomerCode(data.customer().customerCode());

            walletRepository.save(wallet);
            log.info("Wallet successfully activated for user {}", wallet.getUser().getId());
        } else {
            log.warn("No wallet found for email: {}", data.customer().email());
        }
    }

    private void handleAssignFailed(JsonNode data) {
        log.warn("Dedicated account assignment failed: {}", data);
        // find wallet by email if available and mark as FAILED
        String email = data.path("customer").path("email").asString(null);
        if (email != null) {
            walletRepository.findByUserEmail(email).ifPresent(wallet -> {
                wallet.setStatus(WalletStatus.FAILED);
                wallet.setFailureReason("Dedicated account assignment failed");
                walletRepository.save(wallet);
            });
        }
    }

    private void handleChargeSuccess(JsonNode data) {
        log.info("Processing charge.success for wallet funding");

        try {
            // Paystack sends amount in kobo (smallest unit)
            long amountInKobo = data.path("amount").asLong(0);

            BigDecimal amountInNaira = BigDecimal.valueOf(amountInKobo)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_EVEN);   // Fixed here

            String accountNumber = data.path("authorization")
                    .path("receiver_bank_account_number")
                    .asString(null);

            if (accountNumber == null || accountNumber.isBlank()) {
                // Alternative path sometimes used by Paystack
                accountNumber = data.path("authorization")
                        .path("account_number")
                        .asString(null);
            }

            if (accountNumber != null) {
                Optional<Wallet> optionalWallet = walletRepository.findByAccountNumber(accountNumber);

                if (optionalWallet.isPresent()) {
                    Wallet wallet = optionalWallet.get();
                    BigDecimal newBalance = wallet.getBalance().add(amountInNaira);

                    wallet.setBalance(newBalance);
                    walletRepository.save(wallet);

                    log.info("Wallet funded | Account: {} | Amount: ₦{} | New Balance: ₦{}",
                            accountNumber, amountInNaira, newBalance);
                } else {
                    log.warn("No wallet found for account number: {}", accountNumber);
                }
            } else {
                log.warn("Could not extract account number from charge.success payload");
            }
        } catch (Exception e) {
            log.error("Error processing charge.success webhook", e);
        }
    }
}
