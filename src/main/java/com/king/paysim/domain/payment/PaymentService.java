package com.king.paysim.domain.payment;

import com.king.paysim.domain.linkedcard.LinkedCardService;
import com.king.paysim.domain.linkedcard.dto.DirectCardChargeDto;
import com.king.paysim.domain.linkedcard.entity.LinkedCard;
import com.king.paysim.domain.linkedcard.enums.LinkedCardStatus;
import com.king.paysim.domain.payment.dto.*;
import com.king.paysim.domain.transaction.TransactionService;
import com.king.paysim.domain.transaction.dto.CreateTransactionDto;
import com.king.paysim.domain.transaction.enums.TransactionType;
import com.king.paysim.domain.user.entity.User;
import com.king.paysim.domain.wallet.WalletService;
import com.king.paysim.domain.wallet.dto.*;
import com.king.paysim.domain.wallet.entity.Wallet;
import com.king.paysim.domain.wallet.enums.WalletCurrency;
import com.king.paysim.infrastructure.flutterwave.FlutterwaveService;
import com.king.paysim.infrastructure.flutterwave.dto.FlwChargeResponse;
import com.king.paysim.infrastructure.flutterwave.dto.FlwTokenizedChargeResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final LinkedCardService linkedCardService;
    private final WalletService walletService;
    private final FlutterwaveService flutterwaveService;
    private final TransactionService transactionService;

    // ==================== CARD LINKING ====================

    /**
     * Step 1: Initiate direct card charge (for linking)
     */
    public Object initiateCardLinking(DirectCardChargeDto request, String userId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("card_number", request.cardNumber().replace(" ", ""));
        payload.put("cvv", request.cvv());
        payload.put("expiry_month", request.expiryMonth());
        payload.put("expiry_year", request.expiryYear());
        payload.put("amount", request.amount());
        payload.put("currency", "NGN");
        payload.put("country", "NG");
        payload.put("email", request.email());
        payload.put("tx_ref", request.txRef() != null ? request.txRef() : "card-link-" + UUID.randomUUID());
        payload.put("narration", request.narration() != null ? request.narration() : "Card linking on PaySim");
        payload.put("meta", Map.of("userId", userId, "purpose", "card_linking"));

        return flutterwaveService.directCardCharge(payload);
    }

    @Transactional
    public LinkedCard confirmCardLinking(String txRef, User user) {
        return linkedCardService.confirmAndSaveCard(txRef, user);
    }

    // ==================== PAY WITH WALLET BALANCE ====================

    @Transactional
    public TransactionResult payWithWalletBalance(PayWithWalletRequest request, String userId) {
        // Debit from wallet balance
        WithdrawalDto withdrawalDto = WithdrawalDto.builder()
                .amount(request.amount())
                .narration(request.narration())
                .build();

        TransactionResult result = walletService.debitWallet(userId, withdrawalDto);

        log.info("Payment made from wallet balance | UserId={} | Amount={} | Ref={}",
                userId, request.amount(), result.reference());

        return result;
    }

    // ==================== CARD PAYMENTS ====================

    @Transactional
    public TransactionResult fundWalletWithLinkedCard(DepositWithCardRequest request, String userId) {
        LinkedCard card = validateLinkedCard(request.linkedCardId(), userId);

        String txRef = "dep-card-" + UUID.randomUUID().toString().substring(0, 12);

        Map<String, Object> payload = Map.of(
                "token", card.getCardToken(),
                "amount", request.amount(),
                "currency", "NGN",
                "country", "NG",
                "email", request.email(),
                "tx_ref", txRef,
                "narration", request.narration() != null ? request.narration() : "Wallet funding via saved card"
        );

        FlwTokenizedChargeResponse chargeResponse = flutterwaveService.tokenizedCharge(payload);

        if (!"successful".equalsIgnoreCase(chargeResponse.status())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Card charge failed: " + chargeResponse.message());
        }

        Wallet wallet = walletService.find(userId);
        walletService.creditWallet(wallet, chargeResponse.data(), txRef, userId, "Wallet funding via saved card");

        log.info("Wallet funded via linked card | UserId={} | Amount={} | TxRef={}", userId, request.amount(), txRef);

        return new TransactionResult(txRef, request.amount(), WalletCurrency.NGN, wallet.getStatus());
    }

    @Transactional
    public TransactionResult payWithLinkedCard(PayWithCardRequest request, String userId) {
        LinkedCard card = validateLinkedCard(request.linkedCardId(), userId);

        String txRef = "pay_" + UUID.randomUUID().toString().substring(0, 12);

        Map<String, Object> payload = Map.of(
                "token", card.getCardToken(),
                "amount", request.amount(),
                "currency", "NGN",
                "country", "NG",
                "email", request.email(),
                "tx_ref", txRef,
                "narration", request.narration() != null ? request.narration() : "Payment via saved card"
        );

        FlwTokenizedChargeResponse chargeResponse = flutterwaveService.tokenizedCharge(payload);

        if (!"successful".equalsIgnoreCase(chargeResponse.status())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Payment failed: " + chargeResponse.message());
        }

        transactionService.create(CreateTransactionDto.builder()
                .amount(request.amount())
                .currency(WalletCurrency.NGN)
                .transactionType(TransactionType.PAYMENT)
                .reference(txRef)
                .narration(request.narration())
                .providerRef(chargeResponse.data().flw_ref())
                .build(), userId);

        log.info("Direct payment via linked card | UserId={} | Amount={} | TxRef={}", userId, request.amount(), txRef);

        return new TransactionResult(txRef, request.amount(), WalletCurrency.NGN, null);
    }

    // ==================== WALLET OPERATIONS ====================

    @Transactional
    public TransactionResult withdraw(String userId, WithdrawalDto payload, String idempotencyKey) {
        return walletService.withdraw(userId, payload, idempotencyKey);
    }

    @Transactional
    public TransactionResult debitWallet(String userId, WithdrawalDto payload) {
        return walletService.debitWallet(userId, payload);
    }

    @Transactional
    public TransactionResult debitForBillPayment(String userId, BillPaymentDto dto) {
        return walletService.debitForBillPayment(userId, dto);
    }

    // ==================== HELPER ====================

    private LinkedCard validateLinkedCard(String linkedCardId, String userId) {
        LinkedCard card = linkedCardService.getLinkedCardById(linkedCardId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Linked card not found"));

        if (card.getStatus() != LinkedCardStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Card is not active");
        }
        return card;
    }
}