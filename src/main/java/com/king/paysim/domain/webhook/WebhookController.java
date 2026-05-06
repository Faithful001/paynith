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
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Tag(name = "Webhooks", description = "All webhooks")
@Slf4j
@RestController
@RequestMapping("/webhooks")
public class WebhookController {

    private final ObjectMapper objectMapper;
    private final WebhookProviderFactory webhookProviderFactory;
    @Value("${wallet.va.provider}")
    private final String providerName;

    public WebhookController(
            ObjectMapper objectMapper,
            WebhookProviderFactory webhookProviderFactory,
            String providerName
    ) {
        this.objectMapper = objectMapper;
        this.webhookProviderFactory = webhookProviderFactory;
        this.providerName = providerName;
    }

    @Value("${PAYSTACK_SEC_KEY}")
    private String paystackSecretKey;

    @Operation(summary = "Handle webhook call from paystack")
    @PostMapping("/paystack")
    public ResponseEntity<Void> handlePaystackWebhook(
            @RequestHeader(value = "x-paystack-signature", required = false) String signature,
            @RequestBody String payload) {

        if (!isValidSignature(payload, signature)) {
            log.warn("Invalid Paystack webhook signature");
            return ResponseEntity.status(401).build();
        }

        try {
            JsonNode node = objectMapper.readTree(payload);
            String event = node.path("event").asString();
            JsonNode data = node.path("data");

            WebhookProvider provider = webhookProviderFactory.getProvider(ProviderName.valueOf((providerName)));

            provider.handle(event, data);

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error processing Paystack webhook", e);
            return ResponseEntity.status(500).build();
        }
    }

    private boolean isValidSignature(String payload, String signature) {
        if (signature == null || signature.isBlank() || paystackSecretKey == null) {
            log.error("Missing signature or secret key");
            return false;
        }

        try {
            Mac sha512Hmac = Mac.getInstance("HmacSHA512");
            SecretKeySpec keySpec = new SecretKeySpec(
                    paystackSecretKey.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA512"
            );
            sha512Hmac.init(keySpec);

            byte[] hmacBytes = sha512Hmac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String computedSignature = HexFormat.of().formatHex(hmacBytes);

            return computedSignature.equals(signature);

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Error computing HMAC signature", e);
            return false;
        }
    }
}