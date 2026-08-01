package com.ashuboi.payment;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Stub showing how Stripe would plug into {@link PaymentGatewayPort}.
 * Not wired for live keys in this portfolio demo.
 */
@Component
@ConditionalOnProperty(name = "payment.provider", havingValue = "stripe")
public class StripePaymentGatewayStub implements PaymentGatewayPort {

    @Override
    public String providerName() {
        return "stripe";
    }

    @Override
    public CheckoutSession createCheckout(Long userId, BigDecimal amount, String idempotencyKey) {
        throw new UnsupportedOperationException("Stripe adapter stub — set payment.provider=mock for the demo");
    }

    @Override
    public boolean verifyWebhook(String signatureHeader, String rawBody) {
        throw new UnsupportedOperationException("Stripe adapter stub — set payment.provider=mock for the demo");
    }

    @Override
    public PaymentEvent parsePaymentEvent(String rawBody) {
        throw new UnsupportedOperationException("Stripe adapter stub — set payment.provider=mock for the demo");
    }
}
