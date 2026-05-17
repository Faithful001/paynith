package com.king.paysim.domain.wallet;

import com.king.paysim.common.util.JsonUtil;
import com.king.paysim.domain.idempotency.IdempotencyService;
import com.king.paysim.domain.idempotency.entity.Idempotency;
import com.king.paysim.domain.idempotency.enums.IdempotencyStatus;
import com.king.paysim.domain.transaction.TransactionService;
import com.king.paysim.domain.transaction.dto.CreateTransactionDto;
import com.king.paysim.domain.transaction.enums.TransactionType;
import com.king.paysim.domain.user.UserRepository;
import com.king.paysim.domain.user.entity.User;
import com.king.paysim.domain.virtualaccount.dto.VirtualAccountResult;
import com.king.paysim.domain.virtualaccount.enums.ProviderName;
import com.king.paysim.domain.virtualaccount.VirtualAccountProvider;
import com.king.paysim.domain.virtualaccount.VirtualAccountProviderFactory;
import com.king.paysim.domain.wallet.dto.BillPaymentDto;
import com.king.paysim.domain.wallet.dto.CreateWalletDto;
import com.king.paysim.domain.wallet.dto.WithdrawalDto;
import com.king.paysim.domain.wallet.dto.TransactionResult;
import com.king.paysim.domain.wallet.entity.Wallet;
import com.king.paysim.domain.wallet.enums.WalletCurrency;
import com.king.paysim.domain.wallet.enums.WalletStatus;
import com.king.paysim.domain.webhook.dto.FlutterwaveChargeCompletedResult;
import com.king.paysim.infrastructure.flutterwave.FlutterwaveService;
import com.king.paysim.infrastructure.flutterwave.dto.FlwChargeResponse;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.UUID;

@Slf4j
@Service
public class WalletService {

    private final WalletRepository walletRepository;
    private final UserRepository userRepository;
    private final VirtualAccountProviderFactory vAccountProviderFactory;
    private final String providerName;
    private final TransactionService transactionService;
    private final FlutterwaveService flutterwaveService;
    private final IdempotencyService idempotencyService;
    private final String withdrawalHashSecret;

    public WalletService(
            WalletRepository walletRepository,
            UserRepository userRepository,
            VirtualAccountProviderFactory vAccountProviderFactory,
            @Value("${app.va.provider}") String providerName,
            TransactionService transactionService,
            FlutterwaveService flutterwaveService,
            IdempotencyService idempotencyService,
            @Value("WITHDRAWAL_HASH_SEC") String withdrawalHashSecret
    ) {
        this.walletRepository = walletRepository;
        this.userRepository = userRepository;
        this.vAccountProviderFactory = vAccountProviderFactory;
        this.providerName = providerName;
        this.transactionService = transactionService;
        this.flutterwaveService = flutterwaveService;
        this.idempotencyService = idempotencyService;
        this.withdrawalHashSecret = withdrawalHashSecret;
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

        log.info("Virtual Account created for user {} | Bank: {} | Account: {}",
                user.getId(), wallet.getBankName(), wallet.getAccountNumber());

        return walletRepository.save(wallet);
    }

    @Transactional
    public TransactionResult withdraw(
            String userId,
            WithdrawalDto payload,
            String idempotencyKey
    ) {
        String requestHash = generateWithdrawalHash(userId, payload, this.withdrawalHashSecret);

        Idempotency idempotency = idempotencyService.create(
                new Idempotency(
                        null,
                        idempotencyKey,
                        requestHash,
                        null,
                        IdempotencyStatus.PROCESSING,
                        null,
                        null
                )
        );
        try {
            // If already exists, validate request
            idempotencyService.validateRequestHash(idempotency, requestHash);

            log.info("idempotency status: {}", idempotency.getStatus());

            // If already completed, return cached response
            if (idempotencyService.isFinalState(idempotency)) {
                log.info("is final state: {}", idempotency.getStatus());
                return JsonUtil.deserialize(idempotency.getResponseBody(), TransactionResult.class);
            }

            log.info("about to withdraw");

            TransactionResult response = this.doWithdraw(userId, payload);

            idempotency.setResponseBody(JsonUtil.serialize(response));
            idempotencyService.markStatus(IdempotencyStatus.SUCCESS, idempotency);

            return response;

        } catch (Exception ex) {
            idempotencyService.markStatus(IdempotencyStatus.FAILED, idempotency);
            throw ex;
        }
    }

    private TransactionResult doWithdraw(String userId, WithdrawalDto payload) {
        try {
            this.flutterwaveService.initiateTransfer(payload);
        } catch (Exception ex) {
            String message = ex.getMessage() != null ? ex.getMessage() : "Failed to initiate withdrawal";
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, message);
        }

        return debitWallet(userId, payload);
    }

    @Transactional
    public TransactionResult debitWallet (String userId, WithdrawalDto payload){

        Wallet wallet = this.walletRepository
                .findByUserId(userId)
                .orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Wallet not found")
                );

        if (wallet.getBalance().compareTo(payload.amount()) < 0){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient balance");
        }

        wallet.setBalance(wallet.getBalance().subtract(payload.amount()));
        walletRepository.save(wallet);

        String reference = "paysim_withdrawal_" + UUID.randomUUID();

        transactionService.create(CreateTransactionDto.builder()
                        .amount(payload.amount())
                        .currency(WalletCurrency.NGN)
                        .walletId(wallet.getId())
                        .transactionType(TransactionType.WITHDRAWAL)
                        .reference(reference)
                        .narration(payload.narration())
                        .recipientAccountNumber(payload.accountNumber())
                        .recipientAccountName(payload.accountName())
                        .fee(BigDecimal.ZERO)
                        .build(),
                userId);

        return new TransactionResult(
                reference,
                payload.amount(),
                WalletCurrency.NGN,
                wallet.getStatus()
        );
    }

    @Transactional
    public TransactionResult debitForBillPayment(String userId, BillPaymentDto dto) {

        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found"));

        if (wallet.getBalance().compareTo(dto.amount()) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient balance");
        }

        // Debit wallet
        wallet.setBalance(wallet.getBalance().subtract(dto.amount()));
        walletRepository.save(wallet);

        // Create transaction record
        transactionService.create(CreateTransactionDto.builder()
                        .amount(dto.amount())
                        .currency(dto.currency())
                        .walletId(wallet.getId())
                        .transactionType(TransactionType.PAYMENT)
                        .reference(dto.reference())
                        .providerRef(dto.flwRef())
                        .narration(dto.narration())
                        .build(),
                userId);

        log.info("Wallet debited for bill payment | UserId={} | Amount={} | Ref={}",
                userId, dto.amount(), dto.reference());

        return new TransactionResult(dto.reference(), dto.amount(), dto.currency(), wallet.getStatus());
    }

    @Transactional
    public TransactionResult creditWallet(
            Wallet wallet,
            Object chargeData,
            String txRef,
            String userId,
            String narration
    ) {

        BigDecimal amount = BigDecimal.ZERO;
        String flwRef = null;
        String currency = "NGN";

        if (chargeData instanceof FlutterwaveChargeCompletedResult webhook) {
            amount = webhook.amount();
            flwRef = webhook.flw_ref();
            currency = webhook.currency() != null ? webhook.currency().name() : "NGN";

        } else if (chargeData instanceof FlwChargeResponse.FlwChargeData tokenData) {
            amount = tokenData.amount() != null ? tokenData.amount() : BigDecimal.ZERO;
            flwRef = tokenData.flw_ref();
            currency = tokenData.currency() != null ? tokenData.currency() : "NGN";
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid amount received");
        }

        // Credit the wallet
        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepository.save(wallet);

        // Create transaction
        CreateTransactionDto transactionPayload = CreateTransactionDto.builder()
                .amount(amount)
                .currency(WalletCurrency.NGN)
                .walletId(wallet.getId())
                .transactionType(TransactionType.CREDIT)
                .providerRef(flwRef)
                .reference(txRef)
                .narration(narration)
                .fee(BigDecimal.ZERO)
                .build();

        transactionService.create(transactionPayload, userId);

        log.info("Wallet credited | UserId={} | Amount={} | Ref={}", userId, amount, txRef);

        return new TransactionResult(txRef, amount, WalletCurrency.NGN, wallet.getStatus());
    }

    public String generateWithdrawalHash(
            String userId,
            WithdrawalDto dto,
            String secret
    ) {
        try {
            String payload = userId
                    + "|"
                    + dto.amount()
                    + "|"
                    + dto.accountNumber()
                    + "|"
                    + dto.narration();

            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));

            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hash);

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate withdrawal hash", e);
        }
    }

    public Wallet find(String userId) {
        this.userRepository.findById(userId).orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        return this.walletRepository.findByUserId(userId).orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    public Wallet findById(String walletId) {
        return this.walletRepository.findById(walletId).orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found"));
    }


}