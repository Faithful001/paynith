package com.king.paysim.domain.virtual_account.providers;

import com.king.paysim.domain.user.entities.User;
import com.king.paysim.domain.virtual_account.dtos.VirtualAccountResult;
import com.king.paysim.domain.virtual_account.enums.ProviderName;

public interface VirtualAccountProvider {

    VirtualAccountResult createVirtualAccount(User user);

    ProviderName getProviderName();

    default String formatPhone(String phone) {
        if (phone == null) return null;
        String cleaned = phone.replaceAll("[^0-9]", "");
        if (cleaned.startsWith("0")) {
            return "+234" + cleaned.substring(1);
        }
        if (!cleaned.startsWith("234")) {
            return "+234" + cleaned;
        }
        return "+" + cleaned;
    }
}
