package com.king.paysim.domain.webhook.providers;

import com.king.paysim.domain.virtual_account.enums.ProviderName;
import tools.jackson.databind.JsonNode;

public interface WebhookProvider {
    void handle(String event, JsonNode data);
    ProviderName getProviderName();
}
