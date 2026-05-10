package com.king.paysim.domain.virtualaccount;

import com.king.paysim.domain.user.entity.User;
import com.king.paysim.domain.virtualaccount.dto.VirtualAccountResult;
import com.king.paysim.domain.virtualaccount.enums.ProviderName;
import com.king.paysim.domain.wallet.enums.WalletCurrency;

public interface VirtualAccountProvider {

    VirtualAccountResult createVirtualAccount(User user, WalletCurrency currency);

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

//    void initiateTransfer(WithdrawalDto payload);
}
