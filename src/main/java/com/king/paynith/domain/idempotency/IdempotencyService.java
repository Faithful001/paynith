package com.king.paynith.domain.idempotency;

import com.king.paynith.domain.idempotency.entity.Idempotency;
import com.king.paynith.domain.idempotency.enums.IdempotencyStatus;
import jakarta.transaction.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class IdempotencyService {
    private final IdempotencyRepository idempotencyRepository;

    public IdempotencyService(IdempotencyRepository idempotencyRepository){
        this.idempotencyRepository = idempotencyRepository;
    }

    @Transactional
    public Idempotency create(Idempotency entity){
        //check if the idempotency key already exists
        Optional<Idempotency> existing = idempotencyRepository.findByIdempotencyKey(entity.getIdempotencyKey());

        if (existing.isPresent()){
            return existing.get();
        }

        entity.setCreatedAt(LocalDateTime.now());

        return idempotencyRepository.save(entity);
    }

    public Optional<Idempotency> findByKey(String key) {
        return idempotencyRepository.findByIdempotencyKey(key);
    }

    @Transactional
    public void markStatus (IdempotencyStatus status, Idempotency entity) {
        entity.setStatus(status);
        entity.setUpdatedAt(LocalDateTime.now());
        idempotencyRepository.save(entity);
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
