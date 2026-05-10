package com.king.paysim.domain.wallet;

import com.king.paysim.domain.transaction.TransactionService;
import com.king.paysim.domain.transaction.dtos.CreateTransactionDto;
import com.king.paysim.domain.transaction.entities.Transaction;
import com.king.paysim.domain.transaction.enums.TransactionType;
import com.king.paysim.domain.user.UserRepository;
import com.king.paysim.domain.user.entity.User;
import com.king.paysim.domain.virtualaccount.dto.VirtualAccountResult;
import com.king.paysim.domain.virtualaccount.enums.ProviderName;
import com.king.paysim.domain.virtualaccount.VirtualAccountProvider;
import com.king.paysim.domain.virtualaccount.VirtualAccountProviderFactory;
import com.king.paysim.domain.wallet.dto.CreateWalletDto;
import com.king.paysim.domain.wallet.dto.WithdrawalDto;
import com.king.paysim.domain.wallet.entity.Wallet;
import com.king.paysim.domain.wallet.enums.WalletCurrency;
import com.king.paysim.domain.wallet.enums.WalletStatus;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class WalletService {

    private final WalletRepository walletRepository;
    private final UserRepository userRepository;
    private final VirtualAccountProviderFactory vAccountProviderFactory;
    private final String providerName;
    private final TransactionService transactionService;

    public WalletService(
            WalletRepository walletRepository,
            UserRepository userRepository,
            VirtualAccountProviderFactory vAccountProviderFactory,
            @Value("${app.va.provider}") String providerName,
            TransactionService transactionService
    ) {
        this.walletRepository = walletRepository;
        this.userRepository = userRepository;
        this.vAccountProviderFactory = vAccountProviderFactory;
        this.providerName = providerName;
        this.transactionService = transactionService;
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

    public void withdraw(String userId, WithdrawalDto payload){

        Wallet wallet = this.walletRepository.findByUserId(userId).orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found"));

        if (wallet.getBalance().compareTo(payload.amount()) < 0){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient balance");
        }

        wallet.setBalance(wallet.getBalance().subtract(payload.amount()));
        walletRepository.save(wallet);

        String reference = "paysim_withdrawal_" + UUID.randomUUID();

        Transaction transaction = transactionService.create(new CreateTransactionDto(
                payload.amount(),
                WalletCurrency.NGN,
                wallet.getId(),
                TransactionType.WITHDRAWAL,
                Optional.empty(),
                Optional.of(reference),
                payload.narration(),
                Optional.of(payload.accountNumber()),
                Optional.empty(),
                Optional.ofNullable(payload.accountName()),
                BigDecimal.ZERO
        ), userId);
    }

    public Wallet find(String userId) {
        this.userRepository.findById(userId).orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        return this.walletRepository.findByUserId(userId).orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    public Wallet findById(String walletId) {
        return this.walletRepository.findById(walletId).orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found"));
    }


}