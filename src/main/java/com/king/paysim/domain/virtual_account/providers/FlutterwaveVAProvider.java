package com.king.paysim.domain.virtual_account.providers;

import com.king.paysim.domain.user.entities.User;
import com.king.paysim.domain.virtual_account.dtos.FlutterwaveData;
import com.king.paysim.domain.virtual_account.dtos.FlutterwaveVAResponse;
import com.king.paysim.domain.virtual_account.dtos.VirtualAccountResult;
import com.king.paysim.domain.virtual_account.enums.ProviderName;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Slf4j
@Service
public class FlutterwaveVAProvider implements VirtualAccountProvider {
    private final WebClient flwClient;

    public FlutterwaveVAProvider(@Value("${FLUTTERWAVE_SEC_KEY}") String secretKey) {
        log.info("flw secret key " + secretKey);
        this.flwClient = WebClient.builder()
                .baseUrl("https://api.flutterwave.com/v3")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + secretKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .build();
    }

    @Override
    public VirtualAccountResult createVirtualAccount(User user) {
        try {
            Map<String, Object> body = Map.of(
                    "email", user.getEmail(),
                    "tx_ref", "paysim_" + user.getId(),
                    "phonenumber", user.getPhoneNumber(),
                    "is_permanent", true,
                    "firstname", user.getFirstName(),
                    "lastname", user.getLastName(),
                    "narration", "PaySim wallet for " + user.getFirstName(),
                    "bvn", user.getBvn() != null ? user.getBvn() : "12345678901"
            );

            FlutterwaveVAResponse response = flwClient.post().uri("/virtual-account-numbers").bodyValue(body)
                    .retrieve()
                    .onStatus(
                            status -> !status.is2xxSuccessful(),
                            res -> res.bodyToMono(String.class)
                                    .map(err -> {
                                        log.error("Flutterwave VA error: {}", err);
                                        return new RuntimeException(err);
                                    })
                    )
                    .bodyToMono(FlutterwaveVAResponse.class)
                    .block();

            assert response != null;
            FlutterwaveData data = response.data().orElseThrow(() ->
                    new RuntimeException("No data in Flutterwave response"));

            return new VirtualAccountResult(
                    true,
                    data.flw_ref(),
                    data.order_ref(),
                    data.account_number(),
                    data.bank_name(),
                    data.note(),
                    null
            );
        }
        catch(Exception e) {
            return new VirtualAccountResult(
                    false,
                    null,
                    null,
                    null,
                    null,
                    null,
                    e.getMessage());
        }
    }

    @Override
    public ProviderName getProviderName() {
        return ProviderName.FLUTTERWAVE;
    }
}
