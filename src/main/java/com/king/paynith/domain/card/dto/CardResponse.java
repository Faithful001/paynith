package com.king.paynith.domain.card.dto;

import com.king.paynith.domain.card.entity.Card;

import java.time.LocalDateTime;

public record CardResponse(
        String id,
        String last4,
        String brand,
        String expiry,
        String cardType,
        boolean isDefault,
        LocalDateTime linkedAt
) {
    public static CardResponse fromEntity(Card card) {
        return new CardResponse(
                card.getId(),
                card.getLast4(),
                card.getBrand(),
                card.getExpiry(),
                card.getCardType(),
                card.getIsDefault(),
                card.getLinkedAt()
        );
    }
}