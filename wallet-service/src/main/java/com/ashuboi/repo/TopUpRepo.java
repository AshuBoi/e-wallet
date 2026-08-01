package com.ashuboi.repo;

import com.ashuboi.entity.TopUpEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TopUpRepo extends JpaRepository<TopUpEntity, Long> {
    Optional<TopUpEntity> findBySessionId(String sessionId);
    Optional<TopUpEntity> findByProviderAndProviderPaymentId(String provider, String providerPaymentId);
    List<TopUpEntity> findByUserIdOrderByCreatedAtDesc(Long userId);
}
