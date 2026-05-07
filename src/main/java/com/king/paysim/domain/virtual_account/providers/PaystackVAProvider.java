package com.king.paysim.domain.virtual_account.providers;

import com.king.paysim.domain.user.entities.User;
import com.king.paysim.domain.virtual_account.dtos.VirtualAccountResult;
import com.king.paysim.domain.virtual_account.enums.ProviderName;
import com.king.paysim.domain.virtual_account.dtos.CreateVAResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
public class PaystackVAProvider implements VirtualAccountProvider {

    private final WebClient paystackClient;

    public PaystackVAProvider(
            @Value("${PAYSTACK_SEC_KEY}") String secretKey
    ) {
        this.paystackClient = WebClient.builder()
                .baseUrl("https://api.paystack.co")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + secretKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .build();
    }

    @Override
    public VirtualAccountResult createVirtualAccount(User user) {
        try {
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
                    .bodyToMono(CreateVAResponse.class)
                    .block();

            return new VirtualAccountResult(
                    true,
                    null,
                    null,
                    null,
                    null,
                   null,
                    null
            );

        } catch (Exception e) {
            return new VirtualAccountResult(
                    false,
                    null,
                    null,
                    null,
                    null,
                    null,
                    e.getMessage()
            );
        }
    }

    @Override
    public ProviderName getProviderName() {
        return ProviderName.PAYSTACK;
    }
}
