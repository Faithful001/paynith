package com.king.paysim.domain.webhook;

import com.king.paysim.domain.virtual_account.enums.ProviderName;
import com.king.paysim.domain.webhook.providers.WebhookProvider;
import com.king.paysim.domain.webhook.providers.WebhookProviderFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
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

    @Value("${PAYSTACK_SEC_KEY}")
    private String paystackSecretKey;

    @Value("${FLUTTERWAVE_WEBHOOK_HASH}")
    private String flutterwaveSecret;

    public WebhookController(
            ObjectMapper objectMapper,
            WebhookProviderFactory webhookProviderFactory
    ) {
        this.objectMapper = objectMapper;
        this.webhookProviderFactory = webhookProviderFactory;
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