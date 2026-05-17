package com.king.paysim.domain.linkedcard;

import com.king.paysim.domain.linkedcard.entity.LinkedCard;
import com.king.paysim.domain.linkedcard.enums.LinkedCardStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LinkedCardRepository extends JpaRepository<LinkedCard, String> {

    long countByUserId(String userId);

    List<LinkedCard> findByUserIdAndStatus(String userId, LinkedCardStatus status);

    Optional<LinkedCard> findByIdAndUserId(String id, String userId);

    Optional<LinkedCard> findByIdAndUserIdAndStatus(String id, String userId, LinkedCardStatus status);

    Optional<LinkedCard> findByUserIdAndIsDefaultTrue(String userId);

    boolean existsByUserIdAndLast4AndBrand(String userId, String last4, String brand);

    @Modifying
    @Query("UPDATE LinkedCard l SET l.isDefault = false WHERE l.user.id = :userId")
    void resetDefaultCards(@Param("userId") String userId);

    void deleteByIdAndUserId(String id, String userId);
}