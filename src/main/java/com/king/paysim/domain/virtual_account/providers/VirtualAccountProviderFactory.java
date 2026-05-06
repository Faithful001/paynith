package com.king.paysim.domain.virtual_account.providers;
import com.king.paysim.domain.virtual_account.enums.ProviderName;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class VirtualAccountProviderFactory {
    private final Map<ProviderName, VirtualAccountProvider> providers;

    public VirtualAccountProviderFactory(List<VirtualAccountProvider> providers) {
        this.providers = providers.stream()
                .collect(
                        Collectors.toMap(
                                VirtualAccountProvider::getProviderName,
                                Function.identity()
                        )
                );
    }

    public VirtualAccountProvider getProvider(ProviderName provider) {
        return Optional.ofNullable(providers.get(provider))
                .orElseThrow(
                        () -> new IllegalArgumentException("No provider found for name " + provider)
                );
    }
}
