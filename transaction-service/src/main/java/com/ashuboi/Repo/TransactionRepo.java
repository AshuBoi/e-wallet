package com.ashuboi.Repo;

import com.ashuboi.entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TransactionRepo extends JpaRepository<TransactionEntity, Long> {
    Optional<TransactionEntity> findByTxnId(String txnId);
    Optional<TransactionEntity> findByIdempotencyKey(String idempotencyKey);
    List<TransactionEntity> findByFromUserIdOrToUserIdOrderByCreatedAtDesc(Long fromUserId, Long toUserId);
}
