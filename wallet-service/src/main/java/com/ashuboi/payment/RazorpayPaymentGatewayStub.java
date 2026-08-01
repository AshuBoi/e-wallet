package com.ashuboi.payment;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Stub showing how Razorpay would plug into {@link PaymentGatewayPort}.
 */
@Component
@ConditionalOnProperty(name = "payment.provider", havingValue = "razorpay")
public class RazorpayPaymentGatewayStub implements PaymentGatewayPort {

    @Override
    public String providerName() {
        return "razorpay";
    }

    @Override
    public CheckoutSession createCheckout(Long userId, BigDecimal amount, String idempotencyKey) {
        throw new UnsupportedOperationException("Razorpay adapter stub — set payment.provider=mock for the demo");
    }

    @Override
    public boolean verifyWebhook(String signatureHeader, String rawBody) {
        throw new UnsupportedOperationException("Razorpay adapter stub — set payment.provider=mock for the demo");
    }

    @Override
    public PaymentEvent parsePaymentEvent(String rawBody) {
        throw new UnsupportedOperationException("Razorpay adapter stub — set payment.provider=mock for the demo");
    }
}
