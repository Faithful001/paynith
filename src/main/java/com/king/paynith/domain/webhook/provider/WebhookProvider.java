package com.king.paynith.domain.webhook.provider;

import com.king.paynith.domain.virtualaccount.enums.ProviderName;
import tools.jackson.databind.JsonNode;

public interface WebhookProvider {
    void handle(String event, JsonNode data);
    ProviderName getProviderName();
}
