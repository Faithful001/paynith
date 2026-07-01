package com.king.paysim.domain.payment;

import com.king.paysim.domain.card.CardService;
import com.king.paysim.domain.card.dto.DirectCardChargeDto;
import com.king.paysim.domain.card.entity.Card;
import com.king.paysim.domain.card.enums.CardStatus;
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
import com.king.paysim.infrastructure.flutterwave.dto.FlwTokenizedChargeResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final CardService cardService;
    private final WalletService walletService;
    private final FlutterwaveService flutterwaveService;
    private final TransactionService transactionService;

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


    @Transactional
    public TransactionResult payWithLinkedCard(PayWithCardRequest request, String userId) {
        Card card = validateLinkedCard(request.cardId(), userId);

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

        Wallet wallet = walletService.find(userId);

        transactionService.create(CreateTransactionDto.builder()
                .amount(request.amount())
                .currency(WalletCurrency.NGN)
                .walletId(wallet.getId())
                .transactionType(TransactionType.PAYMENT)
                .reference(txRef)
                .narration(request.narration())
                .providerRef(chargeResponse.data().flw_ref())
                .build(), userId);

        log.info("Direct payment via linked card | UserId={} | Amount={} | TxRef={}", userId, request.amount(), txRef);

        return new TransactionResult(txRef, request.amount(), WalletCurrency.NGN, null);
    }

    @Transactional
    public TransactionResult fundWalletWithLinkedCard(DepositWithCardRequest request, String userId) {
        Card card = validateLinkedCard(request.cardId(), userId);

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

    // ==================== HELPER ====================

    private Card validateLinkedCard(String cardId, String userId) {
        Card card = cardService.getCardById(cardId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Linked card not found"));

        if (card.getStatus() != CardStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Card is not active");
        }
        return card;
    }
}