package com.ashuboi.Repo;

import com.ashuboi.entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepo extends JpaRepository<TransactionEntity, Long > {
    TransactionEntity findByTxnId(String txnId);
}
