package com.king.paynith.domain.webhook;

import com.king.paynith.domain.transaction.enums.TransactionType;
import com.king.paynith.domain.virtualaccount.enums.ProviderName;
import com.king.paynith.domain.wallet.WalletRepository;
import com.king.paynith.domain.wallet.WalletService;
import com.king.paynith.domain.wallet.dto.BillPaymentDto;
import com.king.paynith.domain.wallet.dto.WithdrawalDto;
import com.king.paynith.domain.wallet.entity.Wallet;
import com.king.paynith.domain.wallet.enums.WalletCurrency;
import com.king.paynith.domain.webhook.provider.WebhookProvider;
import com.king.paynith.domain.webhook.provider.WebhookProviderFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Tag(name = "Webhooks", description = "All webhooks")
@Slf4j
@RestController
@RequestMapping("/webhooks")
public class WebhookController {

    private final ObjectMapper objectMapper;
    private final WebhookProviderFactory webhookProviderFactory;
    private final WalletService walletService;
    private final WalletRepository walletRepository;

    @Value("${PAYSTACK_SEC_KEY}")
    private String paystackSecretKey;

    @Value("${FLUTTERWAVE_WEBHOOK_HASH}")
    private String flutterwaveSecret;

    public WebhookController(
            ObjectMapper objectMapper,
            WebhookProviderFactory webhookProviderFactory,
            WalletService walletService,
            WalletRepository walletRepository
    ) {
        this.objectMapper = objectMapper;
        this.webhookProviderFactory = webhookProviderFactory;
        this.walletService = walletService;
        this.walletRepository = walletRepository;
    }

    // ===================== FLUTTERWAVE =====================

    @Operation(summary = "Handle Flutterwave webhook")
    @PostMapping("/flutterwave")
    public ResponseEntity<Void> handleFlutterwaveWebhook(
            @RequestHeader(value = "verif-hash", required = false) String signature,
            @RequestBody String payload
    ) {

        log.info("Flutterwave webhook received. Signature: {}", signature);
        log.info("Flutterwave webhook payload: {}", payload);

        if (signature == null || !signature.equals(flutterwaveSecret)) {
            log.warn("Invalid Flutterwave webhook signature");
            return ResponseEntity.status(401).build();
        }

//        if (!isValidHmacSha256(payload, signature, flutterwaveSecret)) {
//            log.warn("Invalid Flutterwave webhook signature");
//            return ResponseEntity.status(401).build();
//        }

        try {
            JsonNode root = objectMapper.readTree(payload);
            String event = root.path("event").asString();
            JsonNode data = root.path("data");

            WebhookProvider provider =
                    webhookProviderFactory.getProvider(ProviderName.FLUTTERWAVE);

            provider.handle(event, data);
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("Flutterwave webhook processing failed", e);
            return ResponseEntity.status(500).build();
        }
    }

    @Operation(summary = "Handle Flutterwave bill payment webhook")
    @PostMapping("/flutterwave/bill-payment")
    public ResponseEntity<Void> handleBillPaymentWebhook(
            @RequestHeader(value = "verif-hash", required = false) String signature,
            @RequestBody String payload
    ) {

        log.info("Flutterwave bill payment webhook received. Signature: {}", signature);

        if (signature == null || !signature.equals(flutterwaveSecret)) {
            log.warn("Invalid Flutterwave webhook signature for bill payment");
            return ResponseEntity.status(401).build();
        }

        try {
            JsonNode root = objectMapper.readTree(payload);
            String event = root.path("event").asString();
            JsonNode data = root.path("data");

            if (!"singlebillpayment.status".equals(event)) {
                log.info("Ignoring non-bill payment event: {}", event);
                return ResponseEntity.ok().build();
            }

            String status = data.path("status").asString();
            String txRef = data.path("tx_ref").asString();
            String flwRef = data.path("flw_ref").asString();

            log.info("Bill payment webhook - tx_ref: {}, status: {}", txRef, status);

            if (!"success".equalsIgnoreCase(status)) {
                log.warn("Bill payment failed for ref: {}", txRef);
                // Optionally update transaction status to FAILED
                return ResponseEntity.ok().build();
            }

            // Extract needed fields
            BigDecimal amount = data.path("amount").decimalValue();
            String customerId = data.path("customer").asString();   // or customer_id
            String network = data.path("network").asString();
            String reference = data.path("reference").asString();   // Flutterwave's reference

            // Extract userId from your tx_ref (e.g. paynith_64f8a1b2 → 64f8a1b2)
            String userId = txRef.replace("paynith_", "");

            BillPaymentDto billDto = BillPaymentDto.builder()
                    .amount(amount)
                    .currency(WalletCurrency.NGN)
                    .reference(txRef)
                    .flwRef(flwRef)
                    .customerId(customerId)
                    .network(network)
                    .narration("Bill payment - " + network)
                    .status(status)
                    .message(data.path("message").asString())
                    .build();

            // Debit wallet and record bill payment
            walletService.debitForBillPayment(userId, billDto);

            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("Bill payment webhook processing failed", e);
            return ResponseEntity.status(500).build();
        }
    }

    // ===================== PAYSTACK =====================

    @Operation(summary = "Handle Paystack webhook")
    @PostMapping("/paystack")
    public ResponseEntity<Void> handlePaystackWebhook(
            @RequestHeader(value = "x-paystack-signature", required = false) String signature,
            @RequestBody String payload
    ) {

        if (!isValidHmacSha512(payload, signature, paystackSecretKey)) {
            log.warn("Invalid Paystack webhook signature");
            return ResponseEntity.status(401).build();
        }

        try {
            JsonNode root = objectMapper.readTree(payload);
            String event = root.path("event").asString();
            JsonNode data = root.path("data");

            WebhookProvider provider =
                    webhookProviderFactory.getProvider(ProviderName.PAYSTACK);

            provider.handle(event, data);
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("Paystack webhook processing failed", e);
            return ResponseEntity.status(500).build();
        }
    }

    // ===================== SIGNATURE UTILS =====================

    private boolean isValidHmacSha256(String payload, String signature, String secret) {
        return isValidHmac(payload, signature, secret, "HmacSHA256");
    }

    private boolean isValidHmacSha512(String payload, String signature, String secret) {
        return isValidHmac(payload, signature, secret, "HmacSHA512");
    }

    private boolean isValidHmac(
            String payload,
            String signature,
            String secret,
            String algorithm
    ) {
        if (signature == null || secret == null) {
            return false;
        }

        try {
            Mac mac = Mac.getInstance(algorithm);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), algorithm));
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String computed = HexFormat.of().formatHex(hash);

            return MessageDigest.isEqual(
                    computed.getBytes(StandardCharsets.UTF_8),
                    signature.getBytes(StandardCharsets.UTF_8)
            );

        } catch (Exception e) {
            log.error("HMAC verification failed", e);
            return false;
        }
    }
}