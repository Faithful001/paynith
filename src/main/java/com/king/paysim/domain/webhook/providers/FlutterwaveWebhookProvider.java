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

            default -> log.debug("Unhandled Flutterwave event: {}", event);
        }
    }

    @Override
    public ProviderName getProviderName() {
        return ProviderName.FLUTTERWAVE;
    }

    // ===================== HANDLERS =====================

    private void handleChargeCompleted(JsonNode data) {
        FlutterwaveChargeCompletedDto dto = convert(data);

        var charge = dto.data();
        if (!"successful".equalsIgnoreCase(charge.status())) {
            log.warn("Charge not successful: {}", charge.status());
            return;
        }

        walletRepository.findByUserEmail(charge.customer().email())
                .ifPresentOrElse(wallet -> {
                    wallet.setBalance(wallet.getBalance().add(BigDecimal.valueOf(charge.amount())));
                    walletRepository.save(wallet);

                    log.info("Wallet funded | Email={} | Amount={}",
                            charge.customer().email(),
                            charge.amount());
                }, () -> log.warn("Wallet not found for {}", charge.customer().email()));
    }

    private void handleTransferCompleted(JsonNode data) {
        log.info("Transfer completed: {}", data.path("id").asText());
    }

    private void handleTransferFailed(JsonNode data) {
        log.warn("Transfer failed: {}", data.path("id").asText());
    }

    private void handleRefundCompleted(JsonNode data) {
        log.info("Refund completed: {}", data.path("id").asText());
    }

    private FlutterwaveChargeCompletedDto convert(JsonNode node) {
        try {
            return objectMapper.treeToValue(node, FlutterwaveChargeCompletedDto.class);
        } catch (Exception e) {
            throw new RuntimeException("Invalid Flutterwave payload", e);
        }
    }
}