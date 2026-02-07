package com.king.paysim.domain.idempotency;

import com.king.paysim.domain.idempotency.entities.PaymentIdempotency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentIdempotencyRepository extends JpaRepository<PaymentIdempotency, Long> {
    Optional<PaymentIdempotency> findByIdempotencyKey (String idempotencyKey);
}
