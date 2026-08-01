package com.ashuboi.service;

import com.ashuboi.dto.TopUpRequest;
import com.ashuboi.dto.TopUpResponse;
import com.ashuboi.dto.WalletBalanceResponse;
import com.ashuboi.entity.TopUpEntity;
import com.ashuboi.entity.Wallet;
import com.ashuboi.exception.InsufficientBalanceException;
import com.ashuboi.exception.ResourceNotFoundException;
import com.ashuboi.payload.TransactionInitPayload;
import com.ashuboi.payload.WalletUpdatedPayload;
import com.ashuboi.payment.CheckoutSession;
import com.ashuboi.payment.PaymentEvent;
import com.ashuboi.payment.PaymentGatewayPort;
import com.ashuboi.repo.TopUpRepo;
import com.ashuboi.repo.WalletRepo;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class WalletService {

    private static final Logger LOGGER = LoggerFactory.getLogger(WalletService.class);
    private static final String WALLET_UPDATED_TOPIC = "WALLET-UPDATED";
    private static final String SCOPE_TOPUP = "TOPUP_CHECKOUT";
    private static final String SCOPE_TOPUP_CREDIT = "TOPUP_CREDIT";

    private final WalletRepo walletRepo;
    private final TopUpRepo topUpRepo;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final PaymentGatewayPort paymentGateway;
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;
    private final BigDecimal initialBalance;

    public WalletService(
            WalletRepo walletRepo,
            TopUpRepo topUpRepo,
            KafkaTemplate<String, Object> kafkaTemplate,
            PaymentGatewayPort paymentGateway,
            IdempotencyService idempotencyService,
            ObjectMapper objectMapper,
            @Value("${wallet.initial-balance:100.00}") BigDecimal initialBalance) {
        this.walletRepo = walletRepo;
        this.topUpRepo = topUpRepo;
        this.kafkaTemplate = kafkaTemplate;
        this.paymentGateway = paymentGateway;
        this.idempotencyService = idempotencyService;
        this.objectMapper = objectMapper;
        this.initialBalance = initialBalance.setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional
    public void createWalletIfAbsent(Long userId, String name, String email) {
        if (walletRepo.existsByUserId(userId)) {
            LOGGER.info("Wallet already exists for userId={}, skipping create", userId);
            return;
        }
        Wallet wallet = Wallet.builder()
                .userId(userId)
                .name(name)
                .email(email)
                .balance(initialBalance)
                .build();
        walletRepo.save(wallet);
        LOGGER.info("Created wallet for userId={} with balance={}", userId, initialBalance);
    }

    public WalletBalanceResponse getBalance(Long userId) {
        Wallet wallet = walletRepo.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for user " + userId));
        return WalletBalanceResponse.builder()
                .userId(wallet.getUserId())
                .name(wallet.getName())
                .email(wallet.getEmail())
                .balance(wallet.getBalance())
                .build();
    }

    @Transactional
    public void doWalletTxn(TransactionInitPayload payload) {
        Wallet fromWallet = walletRepo.findByUserId(payload.getFromUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Sender wallet not found"));
        Wallet toWallet = walletRepo.findByUserId(payload.getToUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Recipient wallet not found"));

        BigDecimal amount = payload.getAmount().setScale(2, RoundingMode.HALF_UP);
        if (fromWallet.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException("Insufficient balance");
        }

        fromWallet.setBalance(fromWallet.getBalance().subtract(amount));
        toWallet.setBalance(toWallet.getBalance().add(amount));
        walletRepo.save(fromWallet);
        walletRepo.save(toWallet);

        publishWalletUpdated(fromWallet);
        publishWalletUpdated(toWallet);
    }

    public TopUpResponse createTopUp(Long userId, TopUpRequest request, String idempotencyKey) {
        return idempotencyService.execute(SCOPE_TOPUP, idempotencyKey, request, TopUpResponse.class, () -> {
            walletRepo.findByUserId(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for user " + userId));

            BigDecimal amount = request.getAmount().setScale(2, RoundingMode.HALF_UP);
            CheckoutSession session = paymentGateway.createCheckout(userId, amount, idempotencyKey);

            TopUpEntity topUp = TopUpEntity.builder()
                    .userId(userId)
                    .amount(amount)
                    .provider(session.provider())
                    .sessionId(session.sessionId())
                    .status("PENDING")
                    .idempotencyKey(idempotencyKey)
                    .build();
            topUpRepo.save(topUp);

            return TopUpResponse.builder()
                    .sessionId(session.sessionId())
                    .provider(session.provider())
                    .checkoutUrl(session.checkoutUrl())
                    .amount(amount)
                    .status("PENDING")
                    .build();
        });
    }

    /**
     * Idempotent credit using provider payment id as the natural idempotency key.
     * Safe under webhook retries from any payment infrastructure.
     */
    @Transactional
    public Map<String, Object> applyPaymentEvent(PaymentEvent event, String rawBody) {
        String key = event.provider() + ":" + event.providerPaymentId();
        Map<String, Object> requestShape = Map.of(
                "provider", event.provider(),
                "providerPaymentId", event.providerPaymentId(),
                "sessionId", event.sessionId(),
                "amount", event.amount(),
                "success", event.success()
        );

        TypeReference<Map<String, Object>> mapType = new TypeReference<>() {};
        return idempotencyService.execute(SCOPE_TOPUP_CREDIT, key, requestShape, mapType, () -> {
            TopUpEntity topUp = topUpRepo.findBySessionId(event.sessionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Top-up session not found: " + event.sessionId()));

            if ("CREDITED".equals(topUp.getStatus())) {
                Map<String, Object> already = new HashMap<>();
                already.put("status", "ALREADY_CREDITED");
                already.put("sessionId", topUp.getSessionId());
                already.put("balance", getBalance(topUp.getUserId()).getBalance());
                return already;
            }

            if (!event.success()) {
                topUp.setStatus("FAILED");
                topUp.setProviderPaymentId(event.providerPaymentId());
                topUpRepo.save(topUp);
                Map<String, Object> failed = new HashMap<>();
                failed.put("status", "FAILED");
                failed.put("sessionId", topUp.getSessionId());
                return failed;
            }

            Wallet wallet = walletRepo.findByUserId(topUp.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));
            BigDecimal amount = event.amount().setScale(2, RoundingMode.HALF_UP);
            wallet.setBalance(wallet.getBalance().add(amount));
            walletRepo.save(wallet);

            topUp.setStatus("CREDITED");
            topUp.setProviderPaymentId(event.providerPaymentId());
            topUpRepo.save(topUp);

            publishWalletUpdated(wallet);

            Map<String, Object> result = new HashMap<>();
            result.put("status", "CREDITED");
            result.put("sessionId", topUp.getSessionId());
            result.put("providerPaymentId", event.providerPaymentId());
            result.put("balance", wallet.getBalance());
            result.put("rawHash", UUID.nameUUIDFromBytes(rawBody.getBytes()).toString());
            return result;
        });
    }

    public void completeMockCheckout(String sessionId, boolean success) {
        TopUpEntity topUp = topUpRepo.findBySessionId(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));
        String providerPaymentId = "mock_pay_" + sessionId;
        try {
            Map<String, Object> body = Map.of(
                    "providerPaymentId", providerPaymentId,
                    "sessionId", sessionId,
                    "userId", topUp.getUserId(),
                    "amount", topUp.getAmount().toPlainString(),
                    "success", success
            );
            PaymentEvent event = paymentGateway.parsePaymentEvent(objectMapper.writeValueAsString(body));
            applyPaymentEvent(event, objectMapper.writeValueAsString(body));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to complete mock checkout", e);
        }
    }

    public List<TopUpEntity> listTopUps(Long userId) {
        return topUpRepo.findByUserIdOrderByCreatedAtDesc(userId);
    }

    private void publishWalletUpdated(Wallet wallet) {
        WalletUpdatedPayload payload = WalletUpdatedPayload.builder()
                .userName(wallet.getName())
                .userEmail(wallet.getEmail())
                .balance(wallet.getBalance())
                .requestId(MDC.get("requestId"))
                .build();
        kafkaTemplate.send(WALLET_UPDATED_TOPIC, String.valueOf(wallet.getUserId()), payload);
        LOGGER.info("Published WALLET-UPDATED for userId={}", wallet.getUserId());
    }
}
