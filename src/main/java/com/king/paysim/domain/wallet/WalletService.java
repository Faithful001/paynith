package com.king.paysim.domain.wallet;

import com.king.paysim.domain.user.UserRepository;
import com.king.paysim.domain.user.entities.User;
import com.king.paysim.domain.wallet.dtos.CreateVAResponse;
import com.king.paysim.domain.wallet.dtos.CreateWalletDto;
import com.king.paysim.domain.wallet.dtos.DedicatedAccountData;
import com.king.paysim.domain.wallet.entities.Wallet;
import com.king.paysim.domain.wallet.enums.WalletStatus;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class WalletService {

    private final WalletRepository walletRepository;
    private final UserRepository userRepository;
    private final WebClient paystackClient;

    public WalletService(WalletRepository walletRepository,
                         UserRepository userRepository,
                         @Value("${PAYSTACK_SEC_KEY}") String secretKey) {
        this.walletRepository = walletRepository;
        this.userRepository = userRepository;

        this.paystackClient = WebClient.builder()
                .baseUrl("https://api.paystack.co")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + secretKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .build();
    }

    @Transactional
    public Wallet create(CreateWalletDto dto) {
        User user = userRepository.findById(dto.userId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (walletRepository.findByUserId(user.getId()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Wallet already exists for this user");
        }

        Wallet wallet = Wallet.builder()
                .user(user)
                .status(WalletStatus.PENDING)
                .balance(BigDecimal.ZERO)
                .build();

        wallet = walletRepository.save(wallet);

        try {
            createPaystackVA(user);
        } catch (Exception e) {
            log.error("Failed to initiate VA creation for user {}", user.getId(), e);
            wallet.setStatus(WalletStatus.FAILED);
            wallet.setFailureReason(e.getMessage());
            walletRepository.save(wallet);
        }

        return wallet;
    }

    private void createPaystackVA(User user) {
        Map<String, Object> body = Map.of(
                "email", user.getEmail(),
                "first_name", user.getFirstName(),
                "last_name", user.getLastName(),
                "phone", formatPhone(user.getPhoneNumber()),
                "preferred_bank", "wema-bank",
                "country", "NG"
        );

        CreateVAResponse response = paystackClient.post()
                .uri("/dedicated_account/assign")
                .bodyValue(body)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        resp -> resp.bodyToMono(String.class)
                                .flatMap(err -> Mono.error(new RuntimeException("Paystack API error: " + err))))
                .bodyToMono(CreateVAResponse.class)
                .block();

        log.info("Dedicated Account assignment initiated for user {}: {}", user.getId(), response);
    }

    private String formatPhone(String phone) {
        if (phone == null) return null;
        String cleaned = phone.replaceAll("[^0-9]", "");
        if (cleaned.startsWith("0")) {
            return "+234" + cleaned.substring(1);
        }
        if (!cleaned.startsWith("234")) {
            return "+234" + cleaned;
        }
        return "+" + cleaned;
    }

    public Wallet getByUserId(String userId) {
        return walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found"));
    }

    // ====================== WEBHOOK HANDLERS ======================

    @Transactional
    public void handlePaystackWebhook(String event, JsonNode data) {
        log.info("Processing Paystack webhook: {}", event);

        try {
            switch (event) {
                case "dedicatedaccount.assign.success":
                    DedicatedAccountData dedicatedData = convertToDedicatedAccountData(data);
                    updateWalletOnSuccess(dedicatedData);
                    break;

                case "dedicatedaccount.assign.failed":
                    updateWalletOnFailure(data);
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

    private DedicatedAccountData convertToDedicatedAccountData(JsonNode node) {
        try {
            return new ObjectMapper().treeToValue(node, DedicatedAccountData.class);
        } catch (Exception e) {
            log.error("Failed to convert JsonNode to DedicatedAccountData", e);
            throw new RuntimeException("Failed to parse dedicated account data", e);
        }
    }

    private void updateWalletOnSuccess(DedicatedAccountData data) {
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
            wallet.setBankSlug(data.bankSlug());
            wallet.setDedicatedAccountId(data.id());
            wallet.setCustomerCode(data.customer().customerCode());

            walletRepository.save(wallet);
            log.info("Wallet successfully activated for user {}", wallet.getUser().getId());
        } else {
            log.warn("No wallet found for email: {}", data.customer().email());
        }
    }

    private void updateWalletOnFailure(JsonNode data) {
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

                    log.info("✅ Wallet funded | Account: {} | Amount: ₦{} | New Balance: ₦{}",
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


    /** Requery a dedicated account */
    public Object requeryDedicatedAccount(String accountNumber) {
        return paystackClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/dedicated_account/requery")
                        .queryParam("account_number", accountNumber)
                        .build())
                .retrieve()
                .bodyToMono(Object.class)
                .block();
    }

    /** Fetch a single dedicated account by Paystack ID */
    public Object fetchDedicatedAccount(Long dedicatedAccountId) {
        return paystackClient.get()
                .uri("/dedicated_account/{id}", dedicatedAccountId)
                .retrieve()
                .bodyToMono(Object.class)
                .block();
    }

    /** List all dedicated accounts (admin only) */
    public Object listDedicatedAccounts(Integer page, Integer perPage) {
        return paystackClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/dedicated_account")
                        .queryParam("page", page != null ? page : 1)
                        .queryParam("per_page", perPage != null ? perPage : 50)
                        .build())
                .retrieve()
                .bodyToMono(Object.class)
                .block();
    }

    /** Deactivate a dedicated account */
    public void deactivateDedicatedAccount(Long dedicatedAccountId) {
        paystackClient.delete()
                .uri("/dedicated_account/{id}", dedicatedAccountId)
                .retrieve()
                .bodyToMono(Void.class)
                .block();
        log.info("Deactivated dedicated account ID: {}", dedicatedAccountId);
    }
}