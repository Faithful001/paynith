package com.king.paysim.domain.idempotency;

import com.king.paysim.domain.idempotency.entities.PaymentIdempotency;
import com.king.paysim.domain.idempotency.enums.IdempotencyStatus;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class PaymentIdempotencyService {
    private final PaymentIdempotencyRepository repository;

    public PaymentIdempotencyService(PaymentIdempotencyRepository repository){
        this.repository = repository;
    }

    @Transactional
    public PaymentIdempotency create(PaymentIdempotency entity){
        //check if the idempotency key already exists
        Optional<PaymentIdempotency> existing = repository.findByIdempotencyKey(entity.getIdempotencyKey());

        if (existing.isPresent()){
            return existing.get();
        }

        entity.setCreatedAt(LocalDateTime.now());

        return repository.save(entity);
    }

    @Transactional
    public Optional<PaymentIdempotency> findByKey(String key) {
        return repository.findByIdempotencyKey(key);
    }

    @Transactional
    public PaymentIdempotency markStatus (IdempotencyStatus status, PaymentIdempotency entity) {
        entity.setStatus(status);
        entity.setUpdatedAt(LocalDateTime.now());
        return repository.save(entity);
    }

    public void validateRequestHash (PaymentIdempotency entity, String requestHash) {
        if (!entity.getRequestHash().equals(requestHash)) {
            throw new IllegalStateException("Idempotency key reused with different request payload");
        }
    }

    public boolean isFinalState (PaymentIdempotency entity) {
        return (entity.getStatus() == IdempotencyStatus.SUCCESS || entity.getStatus() == IdempotencyStatus.FAILED);
    }



}
