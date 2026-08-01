package com.ashuboi;

import com.ashuboi.dto.TopUpRequest;
import com.ashuboi.dto.TopUpResponse;
import com.ashuboi.dto.WalletBalanceResponse;
import com.ashuboi.entity.TopUpEntity;
import com.ashuboi.payment.PaymentEvent;
import com.ashuboi.payment.PaymentGatewayPort;
import com.ashuboi.security.AuthContext;
import com.ashuboi.service.WalletService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/wallet-service")
public class WalletController {

    private static final Logger LOGGER = LoggerFactory.getLogger(WalletController.class);

    private final WalletService walletService;
    private final PaymentGatewayPort paymentGateway;

    public WalletController(WalletService walletService, PaymentGatewayPort paymentGateway) {
        this.walletService = walletService;
        this.paymentGateway = paymentGateway;
    }

    @GetMapping("/hello")
    public ResponseEntity<String> hello() {
        return ResponseEntity.ok("Hello from wallet-service");
    }

    @GetMapping("/balance")
    public ResponseEntity<WalletBalanceResponse> balance() {
        return ResponseEntity.ok(walletService.getBalance(AuthContext.requireUserId()));
    }

    @PostMapping("/topups")
    public ResponseEntity<TopUpResponse> createTopUp(
            @Valid @RequestBody TopUpRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        Long userId = AuthContext.requireUserId();
        LOGGER.info("Creating top-up for userId={} amount={}", userId, request.getAmount());
        return ResponseEntity.ok(walletService.createTopUp(userId, request, idempotencyKey));
    }

    @GetMapping("/topups")
    public ResponseEntity<List<TopUpEntity>> listTopUps() {
        return ResponseEntity.ok(walletService.listTopUps(AuthContext.requireUserId()));
    }

    /**
     * Provider-agnostic webhook ingress. Credits are idempotent on providerPaymentId.
     */
    @PostMapping("/webhooks/{provider}")
    public ResponseEntity<Map<String, Object>> webhook(
            @PathVariable String provider,
            @RequestHeader(value = "X-Signature", required = false) String signature,
            HttpServletRequest request) throws Exception {
        if (!paymentGateway.providerName().equalsIgnoreCase(provider)) {
            throw new IllegalArgumentException("Active provider is " + paymentGateway.providerName()
                    + ", received webhook for " + provider);
        }
        String rawBody = StreamUtils.copyToString(request.getInputStream(), StandardCharsets.UTF_8);
        if (!paymentGateway.verifyWebhook(signature, rawBody)) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid webhook signature"));
        }
        PaymentEvent event = paymentGateway.parsePaymentEvent(rawBody);
        LOGGER.info("Webhook accepted provider={} paymentId={}", event.provider(), event.providerPaymentId());
        return ResponseEntity.ok(walletService.applyPaymentEvent(event, rawBody));
    }

    /** Mock PG checkout page used by the demo UI. */
    @GetMapping(value = "/mock-pg/checkout/{sessionId}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> mockCheckoutPage(@PathVariable String sessionId) {
        String html = """
                <!DOCTYPE html>
                <html><head><meta charset="utf-8"/><title>Mock Payment Gateway</title>
                <style>
                body{font-family:Georgia,serif;background:#0f1c14;color:#e8f0ea;display:grid;place-items:center;min-height:100vh;margin:0}
                .box{max-width:420px;padding:2rem;border:1px solid #2f4a3a;background:#15261c}
                a,button{display:inline-block;margin:0.5rem 0.5rem 0 0;padding:0.7rem 1rem;border:0;cursor:pointer;text-decoration:none;color:#0f1c14;background:#8fbf9f}
                .fail{background:#c97b7b;color:#1a1010}
                </style></head>
                <body><div class="box">
                <h1>Mock Payment Gateway</h1>
                <p>Provider-agnostic demo checkout. Completing this simulates a webhook credit with idempotency.</p>
                <p>Session: %s</p>
                <a href="/wallet-service/mock-pg/complete/%s?success=true">Pay successfully</a>
                <a class="fail" href="/wallet-service/mock-pg/complete/%s?success=false">Fail payment</a>
                </div></body></html>
                """.formatted(sessionId, sessionId, sessionId);
        return ResponseEntity.ok(html);
    }

    @GetMapping(value = "/mock-pg/complete/{sessionId}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> mockComplete(
            @PathVariable String sessionId,
            @RequestParam(defaultValue = "true") boolean success) {
        walletService.completeMockCheckout(sessionId, success);
        String message = success ? "Payment succeeded. Wallet credited (idempotent)." : "Payment failed. No credit applied.";
        String html = """
                <!DOCTYPE html>
                <html><head><meta charset="utf-8"/><title>Payment result</title>
                <style>body{font-family:Georgia,serif;background:#0f1c14;color:#e8f0ea;display:grid;place-items:center;min-height:100vh}
                a{color:#8fbf9f}</style></head>
                <body><div><h1>%s</h1><p><a href="/">Return to app</a></p></div></body></html>
                """.formatted(message);
        return ResponseEntity.ok(html);
    }
}
