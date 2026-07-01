package com.king.paynith.infrastructure.flutterwave;

import com.king.paynith.domain.wallet.dto.WithdrawalDto;
import com.king.paynith.infrastructure.flutterwave.dto.*;
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
import java.util.List;
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

    // for the wallet domain
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

    // get list of banks for Nigeria - wallet domain
    public List<GetBanksResult.Data> getBanks() {
        GetBanksResult result = flwClient.get()
                .uri("/banks/NG")
                .retrieve()
                .bodyToMono(GetBanksResult.class)
                .block();

        assert result != null;
        return result.data();
    }

    // verify bank account before withdrawal - wallet domain
    public VerifyBankAccountResult.Data verifyBankAccount(String accountNumber, String bankCode) {
        Map<String, Object> body = Map.of(
                "account_number", accountNumber,
                "account_bank", bankCode
        );

        log.info("Verifying account | bankCode={} | accountNumber={}", bankCode, accountNumber);

        VerifyBankAccountResult result = flwClient.post()
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

        assert result != null;
        return result.data();
    }

    // get bill categories for a country
    public List<BillCategoryResult.Data> getBillCategories(String country) {
        BillCategoryResult result = flwClient.get()
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

        assert result != null;
        return result.data();
    }

    // get billers for a category
    public List<GetBillerInfoResult.Data> getBillerInfo(String categoryCode, String country) {
        GetBillerInfoResult result = flwClient.get()
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

        assert result != null;
        return result.data();
    }

    // get bill items/packages for a biller
    public List<GetBillInfoResult.Data> getBillInfo(String billerCode) {
        GetBillInfoResult result = flwClient.get()
                .uri("/billers/{billerCode}/items", billerCode)
                .retrieve()
                .onStatus(
                        HttpStatusCode::isError,
                        res -> res.bodyToMono(String.class)
                        .flatMap(
                                err -> Mono.error(
                                        new ResponseStatusException(
                                                HttpStatus.BAD_GATEWAY,
                                                "Flutterwave error: " + err)
                                )
                        )
                )
                .bodyToMono(GetBillInfoResult.class)
                .block();

        assert result != null;
        return result.data();
    }

    // validate customer details before payment
    public ValidateCustomerDetailsResult.Data validateCustomerDetails(String itemCode, String customerId) {
        ValidateCustomerDetailsResult result = flwClient.get()
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

        assert result != null;
        return result.data();
    }

    // create bill payment
    public Object createBillPayment(
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

        CreateBillPaymentResult result = flwClient.post()
                .uri("/billers/{billerCode}/items/{itemCode}/payment", billerCode, itemCode)
                .bodyValue(payload)
                .retrieve()
                .onStatus(HttpStatusCode::isError, res -> res.bodyToMono(String.class)
                        .flatMap(err -> Mono.error(new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Flutterwave error: " + err))))
                .bodyToMono(CreateBillPaymentResult.class)
                .block();

        assert result != null;
        return result.data();
    }

    // get payment status by reference
    public GetPaymentStatusResult.Data getPaymentStatus(String reference) {
        GetPaymentStatusResult result = flwClient.get()
                .uri("/bills/{reference}", reference)
                .retrieve()
                .onStatus(HttpStatusCode::isError, res -> res.bodyToMono(String.class)
                        .flatMap(err -> Mono.error(new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Flutterwave error: " + err))))
                .bodyToMono(GetPaymentStatusResult.class)
                .block();

        assert result != null;
        return result.data();
    }

    // ====================== CARD LINKING & PAYMENTS ======================

    /**
     * Direct Card Charge (First time - for card linking)
     */
    public Object directCardCharge(Map<String, Object> payload) {
        return flwClient.post()
                .uri("/charges?type=card")
                .bodyValue(payload)
                .retrieve()
                .onStatus(HttpStatusCode::isError,
                        res -> res.bodyToMono(String.class)
                                .flatMap(err -> {
                                    log.error("Flutterwave Direct Charge Error: {}", err);
                                    return Mono.error(new ResponseStatusException(HttpStatus.BAD_GATEWAY, err));
                                }))
                .bodyToMono(Object.class)
                .block();
    }

    /**
     * Verify any transaction (used after direct charge or webhook)
     */
    public FlwTransactionResponse verifyTransaction(String txRef) {
        FlwTransactionResponse result = flwClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/transactions/verify_by_reference")
                        .queryParam("tx_ref", txRef)
                        .build())
                .retrieve()
                .onStatus(HttpStatusCode::isError,
                        res -> res.bodyToMono(String.class)
                                .flatMap(err -> Mono.error(new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Verify failed: " + err))))
                .bodyToMono(FlwTransactionResponse.class)
                .block();

        assert result != null;
        return result;
    }

    /**
     * Tokenized Charge - Charge saved card (for deposits)
     */
    public FlwTokenizedChargeResponse tokenizedCharge(Map<String, Object> payload) {
        FlwTokenizedChargeResponse result =flwClient.post()
                .uri("/tokenized-charges")
                .bodyValue(payload)
                .retrieve()
                .onStatus(HttpStatusCode::isError,
                        res -> res.bodyToMono(String.class)
                                .flatMap(err -> {
                                    log.error("Tokenized Charge Error: {}", err);
                                    return Mono.error(new ResponseStatusException(HttpStatus.BAD_GATEWAY, err));
                                }))
                .bodyToMono(FlwTokenizedChargeResponse.class)
                .block();

        assert result != null;
        return result;
    }

    /**
     * Get transactions from flutterwave by ID
     */
    public FlwTransactionResponse.FlwTransactionData getTransactionById(Long transactionId) {
        FlwTransactionResponse result = flwClient.get()
                .uri("/transactions/{id}/verify", transactionId)
                .retrieve()
                .onStatus(HttpStatusCode::isError, res -> res.bodyToMono(String.class)
                        .flatMap(err -> Mono.error(new ResponseStatusException(HttpStatus.BAD_GATEWAY, err))))
                .bodyToMono(FlwTransactionResponse.class)
                .block();

        assert result != null;
        return result.data();
    }

    /**
     * Get transfer status by reference from Flutterwave
     */
    public tools.jackson.databind.JsonNode verifyTransfer(String reference) {
        try {
            return flwClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/transfers")
                            .queryParam("reference", reference)
                            .build())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, res -> res.bodyToMono(String.class)
                            .flatMap(err -> {
                                log.error("Verify transfer failed: {}", err);
                                return Mono.error(new ResponseStatusException(HttpStatus.BAD_GATEWAY, err));
                            }))
                    .bodyToMono(tools.jackson.databind.JsonNode.class)
                    .block();
        } catch (Exception e) {
            log.error("Failed to check transfer status for ref: {}", reference, e);
            return null;
        }
    }
}
