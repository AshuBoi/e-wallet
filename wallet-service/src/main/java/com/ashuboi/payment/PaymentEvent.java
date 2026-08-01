package com.ashuboi.payment;

import java.math.BigDecimal;

public record PaymentEvent(
        String provider,
        String providerPaymentId,
        String sessionId,
        Long userId,
        BigDecimal amount,
        boolean success
) {
}
