package com.ashuboi.payment;

import java.math.BigDecimal;

/**
 * Port that any payment infrastructure (Stripe, Razorpay, PayU, mock) implements.
 * Wallet ledger logic never depends on a concrete provider.
 */
public interface PaymentGatewayPort {
    String providerName();

    CheckoutSession createCheckout(Long userId, BigDecimal amount, String idempotencyKey);

    boolean verifyWebhook(String signatureHeader, String rawBody);

    PaymentEvent parsePaymentEvent(String rawBody);
}
