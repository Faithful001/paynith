package com.king.paysim.infrastructure.flutterwave;

import com.king.paysim.domain.user.entity.User;
import com.king.paysim.infrastructure.flutterwave.dto.FlutterwaveData;
import com.king.paysim.infrastructure.flutterwave.dto.FlutterwaveVAResponse;
import com.king.paysim.domain.virtualaccount.dto.VirtualAccountResult;
import com.king.paysim.domain.virtualaccount.enums.ProviderName;
import com.king.paysim.domain.virtualaccount.VirtualAccountProvider;
import com.king.paysim.domain.wallet.enums.WalletCurrency;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Slf4j
@Service
public class FlutterwaveVAProvider implements VirtualAccountProvider {
    private final WebClient flwClient;

    public FlutterwaveVAProvider(
            @Qualifier("flutterwaveWebClient") WebClient flwClient
    ) {
        this.flwClient = flwClient;
    }

    @Override
    public VirtualAccountResult createVirtualAccount(User user, WalletCurrency currency) {
        log.info("Inside the flw createVirtualAccount method");
        try {
            Map<String, Object> body = Map.of(
                    "email", user.getEmail(),
                    "currency", currency != null ? currency.name() : WalletCurrency.NGN,
                    "tx_ref", "paysim_" + user.getId(),
                    "phonenumber", user.getPhoneNumber(),
                    "is_permanent", true,
                    "firstname", user.getFirstName(),
                    "lastname", user.getLastName(),
                    "narration", "PaySim wallet for " + user.getFirstName(),
                    "bvn", user.getBvn() != null ? user.getBvn() : "12345678901"
            );

            log.info("virtual account numbers flw endpoint about to be called");

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
