package com.king.paysim.domain.transaction;

import com.king.paysim.domain.transaction.entity.Transaction;
import com.king.paysim.domain.transaction.enums.TransactionStatus;
import com.king.paysim.domain.transaction.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, String> {
    boolean existsByReference(String reference);
    Page<Transaction> findByUserId(String id, Pageable pageable);
    Page<Transaction> findByWalletId(String id, Pageable pageable);

    Page<Transaction> findByUserIdAndType(
            String userId,
            TransactionType type,
            Pageable pageable
    );

    Page<Transaction> findByUserIdAndStatus(
            String userId,
            TransactionStatus status,
            Pageable pageable
    );

    Page<Transaction> findByUserIdAndTypeAndStatus(
            String userId,
            TransactionType type,
            TransactionStatus status,
            Pageable pageable
    );
    Optional<Transaction> findByProviderRef(String reference);
    Optional<Transaction> findByReference(String reference);
    @Query("""
        SELECT COALESCE(SUM(t.amount), 0)
        FROM Transaction t
        WHERE t.wallet.id = :walletId
          AND t.type = :type
          AND t.status = :status
    """)
    BigDecimal sumAmountByWalletAndTypeAndStatus(
            @Param("walletId") String walletId,
            @Param("type") TransactionType type,
            @Param("status") TransactionStatus status
    );

}
