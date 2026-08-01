package com.ashuboi.Repo;

import com.ashuboi.entity.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IdempotencyRecordRepo extends JpaRepository<IdempotencyRecord, Long> {
    Optional<IdempotencyRecord> findByScopeAndIdempotencyKey(String scope, String idempotencyKey);
}
