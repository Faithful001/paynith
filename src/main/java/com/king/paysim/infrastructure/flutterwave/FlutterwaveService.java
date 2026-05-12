package com.king.paysim.infrastructure.flutterwave;

import com.king.paysim.domain.wallet.dto.WithdrawalDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Slf4j
@Service
public class FlutterwaveService {
    private final WebClient flwClient;

    public FlutterwaveService(
            @Qualifier("flutterwaveWebClient") WebClient flwClient
    ) {
        this.flwClient = flwClient;
    }

    public void initiateTransfer(WithdrawalDto payload) {
        Map<String, Object> body = Map.of(
                "account_bank", payload.accountBank(),
                "account_number", payload.accountNumber(),
                "amount", payload.amount(),
                "currency", payload.currency(),
                "narration", payload.narration(),
                "debit_currency", "NGN"
        );

        try {
            this.flwClient.post()
                    .uri("/transfers")
                    .bodyValue(body)
                    .retrieve()
                    .onStatus(
                            status -> !status.is2xxSuccessful(),
                            res -> res.bodyToMono(String.class)
                                    .map(err -> {
                                        log.error("Flutterwave error: {}", err);
                                        return new RuntimeException(err);
                                    })
                    )
                    .bodyToMono(Object.class)
                    .block();
            log.info("Transfer initiated | Account={} | Amount={} | Ref={}",
                    payload.accountNumber(), payload.amount(), payload.reference());
        } catch (Exception e) {
            log.error("Failed to initiate transfer", e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to initiate withdrawal");
        }
    }

    // get list of banks for Nigeria
    public Object getBanks() {
        return flwClient.get()
                .uri("/banks/NG")
                .retrieve()
                .bodyToMono(Object.class)
                .block();
    }

    // verify bank account before withdrawal
    public Object verifyBankAccount(String accountNumber, String bankCode) {
        Map<String, Object> body = Map.of(
                "account_number", accountNumber,
                "account_bank", bankCode
        );

        log.info("flutterwave verify-bank request body", body);

        log.info("Verifying account | bankCode={} | accountNumber={}", bankCode, accountNumber);

        return flwClient.post()
                .uri("/accounts/resolve")
                .bodyValue(body)
                .retrieve()
                .onStatus(
                        status -> !status.is2xxSuccessful(),
                        res -> res.bodyToMono(String.class)
                                .map(err -> {
                                    log.error("Flutterwave VERIFY error response: {}", err);
                                    return new ResponseStatusException(
                                            HttpStatus.BAD_GATEWAY,
                                            "Flutterwave error: " + err
                                    );
                                })
                )
                .bodyToMono(Object.class)
                .block();
    }
}
