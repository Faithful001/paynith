package com.king.paynith.domain.card;

import com.king.paynith.domain.card.entity.Card;
import com.king.paynith.domain.card.enums.CardStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CardRepository extends JpaRepository<Card, String> {

    long countByUserId(String userId);

    List<Card> findByUserIdAndStatus(String userId, CardStatus status);

    Optional<Card> findByIdAndUserId(String id, String userId);

    Optional<Card> findByIdAndUserIdAndStatus(String id, String userId, CardStatus status);

    Optional<Card> findByUserIdAndIsDefaultTrue(String userId);

    boolean existsByUserIdAndLast4AndBrand(String userId, String last4, String brand);

    @Modifying
    @Query("UPDATE Card l SET l.isDefault = false WHERE l.user.id = :userId")
    void resetDefaultCards(@Param("userId") String userId);

    void deleteByIdAndUserId(String id, String userId);
}