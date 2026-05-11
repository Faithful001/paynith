package com.king.paysim.infrastructure.flutterwave;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class FlutterwaveClient {
    @Bean("flutterwaveWebClient")
    public WebClient flutterwaveWebClient(@Value("${FLUTTERWAVE_SEC_KEY}") String secretKey) {
        System.out.println("secretKey: " + secretKey);
        return WebClient.builder()
                .baseUrl("https://api.flutterwave.com/v3")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + secretKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .build();
    }
}