package com.king.paysim.domain.webhook.provider;

import com.king.paysim.domain.virtualaccount.enums.ProviderName;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class WebhookProviderFactory {
    private final Map<ProviderName, WebhookProvider> providers;

    public WebhookProviderFactory(List<WebhookProvider> providers){
        this.providers = providers.stream()
                .collect(
                        Collectors
                                .toMap(
                                        WebhookProvider::getProviderName,
                                        Function.identity()
                                )
                );
    }

    public WebhookProvider getProvider(ProviderName provider){
        return Optional.ofNullable(this.providers.get(provider))
                .orElseThrow(()->
                        new IllegalArgumentException("No provider name found for " + provider)
                );
    }
}
