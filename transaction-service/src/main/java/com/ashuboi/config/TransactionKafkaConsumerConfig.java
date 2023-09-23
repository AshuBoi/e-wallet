package com.ashuboi.config;

import com.ashuboi.Repo.TransactionRepo;
import com.ashuboi.entity.TransactionEntity;
import com.ashuboi.TransactionStatusEnum;
import com.ashuboi.payload.TxnCompPayload;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.KafkaListener;

@Configuration
public class TransactionKafkaConsumerConfig {
    private static Logger LOGGER = LoggerFactory.getLogger(TransactionKafkaConsumerConfig.class);

    private static ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private TransactionRepo transactionRepo;

    @KafkaListener(topics = "TXN-COMPLETED", groupId = "txnapp")
    public void consumeFromTxnCompletedTopic(ConsumerRecord payload) throws JsonProcessingException {
        TxnCompPayload txnCompPayload = objectMapper.readValue(payload.value().toString(), TxnCompPayload.class);
        MDC.put("requestId",txnCompPayload.getRequestId());
        LOGGER.info("Getting payload from kafka : {}",payload);
        TransactionEntity transaction = transactionRepo.findById(txnCompPayload.getId()).get();
        if(txnCompPayload.getSuccess()){
            transaction.setStatus(TransactionStatusEnum.SUCCESS);
        }
        else {
            transaction.setStatus(TransactionStatusEnum.FAILED);
        }
        transaction.setReason(txnCompPayload.getReason());
        transactionRepo.save(transaction);
        MDC.clear();
    }
}

