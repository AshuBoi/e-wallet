package com.ashuboi.config;

import com.ashuboi.exception.InsufficientBalanceException;
import com.ashuboi.payload.TransactionInitPayload;
import com.ashuboi.payload.TxnCompPayload;
import com.ashuboi.payload.UserCreatedPayload;
import com.ashuboi.service.WalletService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;

@Configuration
public class KafkaConsumerConfig {

    private static final String TXN_COMPLETED_TOPIC = "TXN-COMPLETED";
    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaConsumerConfig.class);

    private final WalletService walletService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public KafkaConsumerConfig(
            WalletService walletService,
            KafkaTemplate<String, Object> kafkaTemplate,
            ObjectMapper objectMapper) {
        this.walletService = walletService;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "USER-CREATED", groupId = "walletapp")
    public void consumeFromUserCreatedTopic(ConsumerRecord<?, ?> payload) throws Exception {
        UserCreatedPayload userCreatedPayload =
                objectMapper.readValue(payload.value().toString(), UserCreatedPayload.class);
        MDC.put("requestId", userCreatedPayload.getRequestId());
        LOGGER.info("USER-CREATED received for userId={}", userCreatedPayload.getUserId());
        walletService.createWalletIfAbsent(
                userCreatedPayload.getUserId(),
                userCreatedPayload.getUserName(),
                userCreatedPayload.getUserEmail());
        MDC.clear();
    }

    @KafkaListener(topics = "TXN-INIT", groupId = "walletapp")
    public void consumeFromTxnInitTopic(ConsumerRecord<?, ?> payload) throws Exception {
        TransactionInitPayload transactionInitPayload =
                objectMapper.readValue(payload.value().toString(), TransactionInitPayload.class);
        MDC.put("requestId", transactionInitPayload.getRequestId());
        LOGGER.info("TXN-INIT received id={}", transactionInitPayload.getId());

        TxnCompPayload txnCompPayload = TxnCompPayload.builder()
                .id(transactionInitPayload.getId())
                .requestId(transactionInitPayload.getRequestId())
                .build();
        try {
            walletService.doWalletTxn(transactionInitPayload);
            txnCompPayload.setSuccess(Boolean.TRUE);
        } catch (InsufficientBalanceException e) {
            txnCompPayload.setSuccess(Boolean.FALSE);
            txnCompPayload.setReason("Insufficient Balance");
        } catch (Exception e) {
            LOGGER.error("Wallet txn failed", e);
            txnCompPayload.setSuccess(Boolean.FALSE);
            txnCompPayload.setReason("Server Error");
        }
        kafkaTemplate.send(TXN_COMPLETED_TOPIC,
                String.valueOf(transactionInitPayload.getFromUserId()),
                txnCompPayload);
        MDC.clear();
    }
}
