package com.king.paysim.domain.virtual_account.providers;

import com.king.paysim.domain.user.entities.User;
import com.king.paysim.domain.virtual_account.dtos.FlutterwaveData;
import com.king.paysim.domain.virtual_account.dtos.FlutterwaveVAResponse;
import com.king.paysim.domain.virtual_account.dtos.VirtualAccountResult;
import com.king.paysim.domain.virtual_account.enums.ProviderName;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

public class FlutterwaveVAProvider implements VirtualAccountProvider {
    private final WebClient flwClient;

    public FlutterwaveVAProvider(@Value("${PAYSTACK_SEC_KEY}") String secretKey){
        this.flwClient = WebClient.builder()
                .baseUrl("https://developersandbox-api.flutterwave.com")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .build();
    }

    @Override
    public VirtualAccountResult createVirtualAccount(User user) {
        try {
            Map<String, Object> body = Map.of(
                    "customer_id", user.getId(),
                    "amount", 0,
                    "currency", "NGN",
                    "account_type", "static"
            );

            FlutterwaveVAResponse response = flwClient.post().uri("/virtual-accounts").bodyValue(body)
                    .retrieve()
                    .bodyToMono(FlutterwaveVAResponse.class)
                    .block();

            assert response != null;
            FlutterwaveData data = response.data().orElseThrow(() ->
                    new RuntimeException("No data in Flutterwave response"));

            return new VirtualAccountResult(
                    true,
                    data.id(),
                    data.account_number(),
                    data.account_bank_name(),
                    null,
                    null
            );
        }
        catch(Exception e) {
            return new VirtualAccountResult(false, null, null, null, null, e.getMessage());
        }
    }

    @Override
    public ProviderName getProviderName() {
        return ProviderName.FLUTTERWAVE;
    }
}
