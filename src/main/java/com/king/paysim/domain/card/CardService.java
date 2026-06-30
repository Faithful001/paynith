package com.king.paysim.domain.card;

import com.king.paysim.domain.card.dto.DirectCardChargeDto;
import com.king.paysim.domain.card.dto.LinkedCardResponse;
import com.king.paysim.domain.card.entity.Card;
import com.king.paysim.domain.card.enums.CardStatus;
import com.king.paysim.domain.user.entity.User;
import com.king.paysim.infrastructure.flutterwave.FlutterwaveService;
import com.king.paysim.infrastructure.flutterwave.dto.FlwTransactionResponse;
import jakarta.transaction.Transactional;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class CardService {
    private final FlutterwaveService flwService;
    private final CardRepository cardRepository;

    public CardService(
            FlutterwaveService flwService,
            CardRepository cardRepository
    ){
        this.flwService = flwService;
        this.cardRepository = cardRepository;

    }

    public Object initiateDirectCardCharge(DirectCardChargeDto req, String userId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("card_number", req.cardNumber().replace(" ", ""));
        payload.put("cvv", req.cvv());
        payload.put("expiry_month", req.expiryMonth());
        payload.put("expiry_year", req.expiryYear());
        payload.put("amount", req.amount());
        payload.put("currency", "NGN");
        payload.put("country", "NG");
        payload.put("email", req.email());
        payload.put("tx_ref", req.txRef());
        payload.put("narration", "Card linking on PaySim");
        payload.put("meta", Map.of("userId", userId, "purpose", "card_linking"));

        return flwService.directCardCharge(payload);
    }

    @Transactional
    public Card confirmAndSaveCard(String txRef, User user) {
        FlwTransactionResponse tx = flwService.verifyTransaction(txRef);

        if (!"successful".equalsIgnoreCase(tx.status())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Transaction not successful");
        }

        var cardData = tx.data().card();
        if (cardData == null || StringUtils.isBlank(cardData.token())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Card token not received");
        }

        Card card = Card.builder()
                .user(user)
                .cardToken(cardData.token())
                .last4(cardData.last4digits())
                .expiry(cardData.expiry())
                .brand(cardData.brand())
                .cardType(cardData.type())
                .status(CardStatus.ACTIVE)
                .isDefault(cardRepository.countByUserId(user.getId()) == 0)
                .build();

        return cardRepository.save(card);
    }

    public Optional<Card> getLinkedCardById(String id, String userId) {
        return cardRepository.findByIdAndUserIdAndStatus(id, userId, CardStatus.ACTIVE);
    }

    public List<LinkedCardResponse> getUserLinkedCards(String userId) {
        return cardRepository.findByUserIdAndStatus(userId, CardStatus.ACTIVE)
                .stream().map(LinkedCardResponse::fromEntity).toList();
    }

    @Transactional
    public Card setDefaultCard(String userId, String cardId) {
        // Reset all cards to non-default
        cardRepository.resetDefaultCards(userId);

        return cardRepository.findByIdAndUserId(cardId, userId)
                .map(card -> {
                    card.setIsDefault(true);
                    return cardRepository.save(card);
                })
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Card not found"));
    }

    @Transactional
    public void deleteLinkedCard(String userId, String cardId) {
        cardRepository.deleteByIdAndUserId(cardId, userId);
    }
}
