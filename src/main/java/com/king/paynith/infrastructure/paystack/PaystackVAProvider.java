package com.king.paynith.infrastructure.paystack;

import com.king.paynith.domain.user.entity.User;
import com.king.paynith.domain.virtualaccount.dto.VirtualAccountResult;
import com.king.paynith.domain.virtualaccount.enums.ProviderName;
import com.king.paynith.domain.virtualaccount.dto.CreateVAResponse;
import com.king.paynith.domain.virtualaccount.VirtualAccountProvider;
import com.king.paynith.domain.wallet.enums.WalletCurrency;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
public class PaystackVAProvider implements VirtualAccountProvider {

    private final WebClient paystackClient;

    public PaystackVAProvider(
            @Qualifier("paystackWebClient") WebClient paystackClient
    ) {
        this.paystackClient = paystackClient;
    }

    @Override
    public VirtualAccountResult createVirtualAccount(User user, WalletCurrency currency) {
        try {
            Map<String, Object> body = Map.of(
                    "email", user.getEmail(),
                    "first_name", user.getFirstName(),
                    "last_name", user.getLastName(),
                    "phone", formatPhone(user.getPhoneNumber()),
                    "preferred_bank", "wema-bank",
                    "country", currency
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
