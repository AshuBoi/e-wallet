package com.ashuboi.service;

import com.ashuboi.entity.IdempotencyRecord;
import com.ashuboi.exception.IdempotencyConflictException;
import com.ashuboi.idempotency.IdempotencyHasher;
import com.ashuboi.Repo.IdempotencyRecordRepo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.function.Supplier;

@Service
public class IdempotencyService {

    private final IdempotencyRecordRepo repo;
    private final ObjectMapper objectMapper;

    public IdempotencyService(IdempotencyRecordRepo repo, ObjectMapper objectMapper) {
        this.repo = repo;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public <T> T execute(String scope, String idempotencyKey, Object requestBody, Class<T> responseType, Supplier<T> action) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Idempotency-Key header is required");
        }

        String requestHash = IdempotencyHasher.hashRequest(requestBody);
        Optional<IdempotencyRecord> existing = repo.findByScopeAndIdempotencyKey(scope, idempotencyKey);
        if (existing.isPresent()) {
            IdempotencyRecord record = existing.get();
            if (!record.getRequestHash().equals(requestHash)) {
                throw new IdempotencyConflictException(
                        "Idempotency-Key reused with a different request body");
            }
            try {
                return objectMapper.readValue(record.getResponseBody(), responseType);
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("Corrupt idempotency response cache", e);
            }
        }

        T response = action.get();
        try {
            IdempotencyRecord record = IdempotencyRecord.builder()
                    .scope(scope)
                    .idempotencyKey(idempotencyKey)
                    .requestHash(requestHash)
                    .status("COMPLETED")
                    .responseBody(objectMapper.writeValueAsString(response))
                    .build();
            repo.save(record);
            return response;
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to serialize idempotency response", e);
        }
    }
}
