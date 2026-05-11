package com.king.paysim.domain.idempotency;

import com.king.paysim.domain.idempotency.entity.Idempotency;
import com.king.paysim.domain.idempotency.enums.IdempotencyStatus;
import jakarta.transaction.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
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
    public Idempotency create(Idempotency entity) {
        try {
            return repository.save(entity);
        } catch (DataIntegrityViolationException ex) {

            // Another request already created it
            return repository.findByIdempotencyKey(entity.getIdempotencyKey())
                    .orElseThrow(() ->
                            new IllegalStateException(
                                    "Idempotency record expected but not found for key: "
                                            + entity.getIdempotencyKey()
                            )
                    );
        }
    }

    public Optional<Idempotency> findByKey(String key) {
        return repository.findByIdempotencyKey(key);
    }

    @Transactional
    public void markStatus (IdempotencyStatus status, Idempotency entity) {
        entity.setStatus(status);
        entity.setUpdatedAt(LocalDateTime.now());
        repository.save(entity);
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
