package com.king.paysim.domain.webhook.provider;

import com.king.paysim.domain.virtualaccount.enums.ProviderName;
import tools.jackson.databind.JsonNode;

public interface WebhookProvider {
    void handle(String event, JsonNode data);
    ProviderName getProviderName();
}
