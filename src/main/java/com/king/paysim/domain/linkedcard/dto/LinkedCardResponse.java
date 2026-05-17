package com.king.paysim.domain.linkedcard.dto;

import com.king.paysim.domain.linkedcard.entity.LinkedCard;

import java.time.LocalDateTime;

public record LinkedCardResponse(
        String id,
        String last4,
        String brand,
        String expiry,
        String cardType,
        boolean isDefault,
        LocalDateTime linkedAt
) {
    public static LinkedCardResponse fromEntity(LinkedCard card) {
        return new LinkedCardResponse(
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