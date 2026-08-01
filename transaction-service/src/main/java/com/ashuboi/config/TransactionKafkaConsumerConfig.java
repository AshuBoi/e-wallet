package com.ashuboi.config;

import com.ashuboi.Repo.TransactionRepo;
import com.ashuboi.TransactionStatusEnum;
import com.ashuboi.entity.TransactionEntity;
import com.ashuboi.payload.TxnCompPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class TransactionKafkaConsumerConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionKafkaConsumerConfig.class);

    private final TransactionRepo transactionRepo;
    private final ObjectMapper objectMapper;

    @Value("${kafka.bootstrapAddress}")
    private String bootStrapServers;

    public TransactionKafkaConsumerConfig(TransactionRepo transactionRepo, ObjectMapper objectMapper) {
        this.transactionRepo = transactionRepo;
        this.objectMapper = objectMapper;
    }

    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootStrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        return factory;
    }

    @KafkaListener(topics = "TXN-COMPLETED", groupId = "txnapp")
    public void consumeFromTxnCompletedTopic(ConsumerRecord<?, ?> payload) throws Exception {
        TxnCompPayload txnCompPayload = objectMapper.readValue(payload.value().toString(), TxnCompPayload.class);
        MDC.put("requestId", txnCompPayload.getRequestId());
        TransactionEntity transaction = transactionRepo.findById(txnCompPayload.getId())
                .orElse(null);
        if (transaction == null) {
            LOGGER.warn("TXN-COMPLETED for unknown id={}", txnCompPayload.getId());
            MDC.clear();
            return;
        }
        if (Boolean.TRUE.equals(txnCompPayload.getSuccess())) {
            transaction.setStatus(TransactionStatusEnum.SUCCESS);
        } else {
            transaction.setStatus(TransactionStatusEnum.FAILED);
            transaction.setReason(txnCompPayload.getReason());
        }
        transactionRepo.save(transaction);
        LOGGER.info("Updated txn id={} status={}", transaction.getId(), transaction.getStatus());
        MDC.clear();
    }
}
