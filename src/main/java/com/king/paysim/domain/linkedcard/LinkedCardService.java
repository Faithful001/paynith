package com.king.paysim.domain.linkedcard;

import com.king.paysim.domain.linkedcard.dto.DirectCardChargeDto;
import com.king.paysim.domain.linkedcard.dto.LinkedCardResponse;
import com.king.paysim.domain.linkedcard.entity.LinkedCard;
import com.king.paysim.domain.linkedcard.enums.LinkedCardStatus;
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
public class LinkedCardService {
    private final FlutterwaveService flwService;
    private final LinkedCardRepository linkedCardRepository;

    public LinkedCardService(
            FlutterwaveService flwService,
            LinkedCardRepository linkedCardRepository
    ){
        this.flwService = flwService;
        this.linkedCardRepository = linkedCardRepository;

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
    public LinkedCard confirmAndSaveCard(String txRef, User user) {
        FlwTransactionResponse tx = flwService.verifyTransaction(txRef);

        if (!"successful".equalsIgnoreCase(tx.status())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Transaction not successful");
        }

        var cardData = tx.data().card();
        if (cardData == null || StringUtils.isBlank(cardData.token())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Card token not received");
        }

        LinkedCard linkedCard = LinkedCard.builder()
                .user(user)
                .cardToken(cardData.token())
                .last4(cardData.last4digits())
                .expiry(cardData.expiry())
                .brand(cardData.brand())
                .cardType(cardData.type())
                .status(LinkedCardStatus.ACTIVE)
                .isDefault(linkedCardRepository.countByUserId(user.getId()) == 0)
                .build();

        return linkedCardRepository.save(linkedCard);
    }

    public Optional<LinkedCard> getLinkedCardById(String id, String userId) {
        return linkedCardRepository.findByIdAndUserIdAndStatus(id, userId, LinkedCardStatus.ACTIVE);
    }

    public List<LinkedCardResponse> getUserLinkedCards(String userId) {
        return linkedCardRepository.findByUserIdAndStatus(userId, LinkedCardStatus.ACTIVE)
                .stream().map(LinkedCardResponse::fromEntity).toList();
    }

    @Transactional
    public LinkedCard setDefaultCard(String userId, String cardId) {
        // Reset all cards to non-default
        linkedCardRepository.resetDefaultCards(userId);

        return linkedCardRepository.findByIdAndUserId(cardId, userId)
                .map(card -> {
                    card.setIsDefault(true);
                    return linkedCardRepository.save(card);
                })
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Card not found"));
    }

    @Transactional
    public void deleteLinkedCard(String userId, String cardId) {
        linkedCardRepository.deleteByIdAndUserId(cardId, userId);
    }
}
