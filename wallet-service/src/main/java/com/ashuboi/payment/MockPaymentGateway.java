package com.ashuboi.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "payment.provider", havingValue = "mock", matchIfMissing = true)
public class MockPaymentGateway implements PaymentGatewayPort {

    private final String publicBaseUrl;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MockPaymentGateway(@Value("${payment.public-base-url}") String publicBaseUrl) {
        this.publicBaseUrl = publicBaseUrl;
    }

    @Override
    public String providerName() {
        return "mock";
    }

    @Override
    public CheckoutSession createCheckout(Long userId, BigDecimal amount, String idempotencyKey) {
        String sessionId = "mock_sess_" + UUID.randomUUID();
        String checkoutUrl = publicBaseUrl + "/wallet-service/mock-pg/checkout/" + sessionId;
        return new CheckoutSession(providerName(), sessionId, checkoutUrl, amount, userId);
    }

    @Override
    public boolean verifyWebhook(String signatureHeader, String rawBody) {
        // Demo provider accepts unsigned or "mock-signature" headers.
        return signatureHeader == null || signatureHeader.isBlank() || "mock-signature".equals(signatureHeader);
    }

    @Override
    public PaymentEvent parsePaymentEvent(String rawBody) {
        try {
            JsonNode node = objectMapper.readTree(rawBody);
            return new PaymentEvent(
                    providerName(),
                    node.path("providerPaymentId").asText(),
                    node.path("sessionId").asText(),
                    node.path("userId").asLong(),
                    new BigDecimal(node.path("amount").asText()),
                    node.path("success").asBoolean(true)
            );
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid mock webhook payload", e);
        }
    }
}
