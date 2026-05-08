package com.king.paysim.domain.wallet;

import com.king.paysim.domain.user.UserRepository;
import com.king.paysim.domain.user.entities.User;
import com.king.paysim.domain.virtual_account.dtos.VirtualAccountResult;
import com.king.paysim.domain.virtual_account.enums.ProviderName;
import com.king.paysim.domain.virtual_account.providers.VirtualAccountProvider;
import com.king.paysim.domain.virtual_account.providers.VirtualAccountProviderFactory;
import com.king.paysim.domain.wallet.dtos.CreateWalletDto;
import com.king.paysim.domain.wallet.entities.Wallet;
import com.king.paysim.domain.wallet.enums.WalletCurrency;
import com.king.paysim.domain.wallet.enums.WalletStatus;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class WalletService {

    private final WalletRepository walletRepository;
    private final UserRepository userRepository;
    private final VirtualAccountProviderFactory vAccountProviderFactory;
    private final String providerName;

    public WalletService(
            WalletRepository walletRepository,
            UserRepository userRepository,
            VirtualAccountProviderFactory vAccountProviderFactory,
            @Value("${app.va.provider}") String providerName
    ) {
        this.walletRepository = walletRepository;
        this.userRepository = userRepository;
        this.vAccountProviderFactory = vAccountProviderFactory;
        this.providerName = providerName;
    }

    @Transactional
    public Wallet create(CreateWalletDto dto) {
        User user = userRepository.findById(dto.userId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (walletRepository.findByUserId(user.getId()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Wallet already exists for this user");
        }

        Wallet wallet = Wallet.builder()
                .user(user)
                .status(WalletStatus.PENDING)
                .balance(BigDecimal.ZERO)
                .build();

        wallet = walletRepository.save(wallet);

        VirtualAccountProvider provider = this.vAccountProviderFactory
                .getProvider(ProviderName.valueOf((providerName.toUpperCase())));

        VirtualAccountResult result = provider.createVirtualAccount(user, wallet.getCurrency());

        if (result.success()) {
            wallet.setStatus(WalletStatus.ACTIVE);
            wallet.setAccountNumber(result.accountNumber());
            wallet.setBankName(result.bankName());
            wallet.setCurrency(WalletCurrency.NGN);
            wallet.setProviderRef(result.providerRef());
            wallet.setOrderRef(result.orderRef());
            wallet.setPaymentNote(result.paymentNote());
        } else {
            wallet.setStatus(WalletStatus.FAILED);
            wallet.setFailureReason(result.errorMessage());
        }

        return walletRepository.save(wallet);
    }

    public List<Wallet> findAll(String userId) {
        this.userRepository.findById(userId).orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        return this.walletRepository.findAllByUserId(userId);
    }


}