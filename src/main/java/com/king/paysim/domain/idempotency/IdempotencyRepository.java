package com.king.paysim.domain.idempotency;

import com.king.paysim.domain.idempotency.entities.Idempotency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IdempotencyRepository extends JpaRepository<Idempotency, String> {
    Optional<Idempotency> findByIdempotencyKey (String idempotencyKey);
}
