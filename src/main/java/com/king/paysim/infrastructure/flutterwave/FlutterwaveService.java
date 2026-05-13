package com.king.paysim.infrastructure.flutterwave;

import com.king.paysim.domain.wallet.dto.WithdrawalDto;
import com.king.paysim.infrastructure.flutterwave.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.HashMap;
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
                            HttpStatusCode::isError,
                            res -> res.bodyToMono(String.class)
                                    .flatMap(err -> {
                                        log.error("Flutterwave error: {}", err);
                                        return Mono.error(new RuntimeException(err));
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
    public GetBanksResult getBanks() {
        return flwClient.get()
                .uri("/banks/NG")
                .retrieve()
                .bodyToMono(GetBanksResult.class)
                .block();
    }

    // verify bank account before withdrawal
    public VerifyBankAccountResult verifyBankAccount(String accountNumber, String bankCode) {
        Map<String, Object> body = Map.of(
                "account_number", accountNumber,
                "account_bank", bankCode
        );

        log.info("Verifying account | bankCode={} | accountNumber={}", bankCode, accountNumber);

        return flwClient.post()
                .uri("/accounts/resolve")
                .bodyValue(body)
                .retrieve()
                .onStatus(
                        HttpStatusCode::isError,
                        res -> res.bodyToMono(String.class)
                                .flatMap(err -> {
                                    log.error("Flutterwave VERIFY error response: {}", err);
                                    return Mono.error(new ResponseStatusException(
                                            HttpStatus.BAD_GATEWAY,
                                            "Flutterwave error: " + err
                                    ));
                                })
                )
                .bodyToMono(VerifyBankAccountResult.class)
                .block();
    }

    // get bill categories for a country
    public BillCategoryResult getBillCategories(String country) {
        return flwClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/top-bill-categories")
                        .queryParam("country", country)
                        .build()
                )
                .retrieve()
                .onStatus(HttpStatusCode::isError, res -> res.bodyToMono(String.class)
                        .flatMap(err -> Mono.error(new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Flutterwave error: " + err))))
                .bodyToMono(BillCategoryResult.class)
                .block();
    }

    // get billers for a category
    public GetBillerInfoResult getBillerInfo(String categoryCode, String country) {
        return flwClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/bills/{categoryCode}/billers")
                        .queryParam("country", country)
                        .build(categoryCode)
                )
                .retrieve()
                .onStatus(HttpStatusCode::isError, res -> res.bodyToMono(String.class)
                        .flatMap(err -> Mono.error(new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Flutterwave error: " + err))))
                .bodyToMono(GetBillerInfoResult.class)
                .block();
    }

    // get bill items/packages for a biller
    public GetBillInfoResult getBillInfo(String billerCode) {
        return flwClient.get()
                .uri("/billers/{billerCode}/items", billerCode)
                .retrieve()
                .onStatus(HttpStatusCode::isError, res -> res.bodyToMono(String.class)
                        .flatMap(err -> Mono.error(new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Flutterwave error: " + err))))
                .bodyToMono(GetBillInfoResult.class)
                .block();
    }

    // validate customer details before payment
    public ValidateCustomerDetailsResult validateCustomerDetails(String itemCode, String customerId) {
        return flwClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/bill-items/{itemCode}/validate")
                        .queryParam("customer", customerId)
                        .build(itemCode)
                )
                .retrieve()
                .onStatus(HttpStatusCode::isError, res -> res.bodyToMono(String.class)
                        .flatMap(err -> Mono.error(new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Flutterwave error: " + err))))
                .bodyToMono(ValidateCustomerDetailsResult.class)
                .block();
    }

    // create bill payment
    public CreateBillPaymentResult createBillPayment(
            String billerCode,
            String itemCode,
            String country,
            String customerId,
            BigDecimal amount,
            String reference,
            String callbackUrl
    ) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("country", country);
        payload.put("customer_id", customerId);
        payload.put("amount", amount);
        payload.put("reference", reference);
        if (callbackUrl != null) payload.put("callback_url", callbackUrl);

        return flwClient.post()
                .uri("/billers/{billerCode}/items/{itemCode}/payment", billerCode, itemCode)
                .bodyValue(payload)
                .retrieve()
                .onStatus(HttpStatusCode::isError, res -> res.bodyToMono(String.class)
                        .flatMap(err -> Mono.error(new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Flutterwave error: " + err))))
                .bodyToMono(CreateBillPaymentResult.class)
                .block();
    }

    // get payment status by reference
    public GetPaymentStatusResult getPaymentStatus(String reference) {
        return flwClient.get()
                .uri("/bills/{reference}", reference)
                .retrieve()
                .onStatus(HttpStatusCode::isError, res -> res.bodyToMono(String.class)
                        .flatMap(err -> Mono.error(new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Flutterwave error: " + err))))
                .bodyToMono(GetPaymentStatusResult.class)
                .block();
    }
}
