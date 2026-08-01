package com.ashuboi.payment;

import java.math.BigDecimal;

public record CheckoutSession(
        String provider,
        String sessionId,
        String checkoutUrl,
        BigDecimal amount,
        Long userId
) {
}
