package com.king.paysim.domain.idempotency;

import com.king.paysim.domain.idempotency.entities.Idempotency;
import com.king.paysim.domain.idempotency.enums.IdempotencyStatus;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class IdempotencyService {
    private final IdempotencyRepository repository;

    public IdempotencyService(IdempotencyRepository repository){
        this.repository = repository;
    }

    @Transactional
    public Idempotency create(Idempotency entity){
        //check if the idempotency key already exists
        Optional<Idempotency> existing = repository.findByIdempotencyKey(entity.getIdempotencyKey());

        if (existing.isPresent()){
            return existing.get();
        }

        entity.setCreatedAt(LocalDateTime.now());

        return repository.save(entity);
    }

    @Transactional
    public Optional<Idempotency> findByKey(String key) {
        return repository.findByIdempotencyKey(key);
    }

    @Transactional
    public Idempotency markStatus (IdempotencyStatus status, Idempotency entity) {
        entity.setStatus(status);
        entity.setUpdatedAt(LocalDateTime.now());
        return repository.save(entity);
    }

    public void validateRequestHash (Idempotency entity, String requestHash) {
        if (!entity.getRequestHash().equals(requestHash)) {
            throw new IllegalStateException("Idempotency key reused with different request payload");
        }
    }

    public boolean isFinalState (Idempotency entity) {
        return (entity.getStatus() == IdempotencyStatus.SUCCESS || entity.getStatus() == IdempotencyStatus.FAILED);
    }



}
