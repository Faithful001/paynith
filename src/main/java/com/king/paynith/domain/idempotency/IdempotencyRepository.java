package com.king.paynith.domain.idempotency;

import com.king.paynith.domain.idempotency.entity.Idempotency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IdempotencyRepository extends JpaRepository<Idempotency, String> {
    Optional<Idempotency> findByIdempotencyKey (String idempotencyKey);
}
