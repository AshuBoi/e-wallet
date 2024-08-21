package com.ashuboi;

import com.ashuboi.payload.UserCreatedPayload;
import com.ashuboi.payload.WalletUpdatedPayload;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@Configuration
public class
KafkaNotifyConsumerConfig {

    private static Logger LOGGER = LoggerFactory.getLogger(KafkaNotifyConsumerConfig.class);


    private static ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private JavaMailSender javaMailSender;

    @KafkaListener(topics = "USER-CREATED", groupId = "email")
    public void consumeFromUserCreatedTopic(ConsumerRecord payload) throws JsonProcessingException {
        UserCreatedPayload userCreatedPayload = objectMapper.readValue(payload.value().toString(),UserCreatedPayload.class);
        MDC.put("requestId",userCreatedPayload.getRequestId());
        LOGGER.info("Getting payload from kafka : {}",payload);

        SimpleMailMessage simpleMailMessage = new SimpleMailMessage();
        simpleMailMessage.setFrom("ashuboiemailsender@gmail.com");
        simpleMailMessage.setSubject("Welcome "+userCreatedPayload.getUserName()+"!");
        simpleMailMessage.setTo(userCreatedPayload.getUserEmail());
        simpleMailMessage.setText("Hi "+userCreatedPayload.getUserName()+", Welcome to AshuBoi's wallet world");
        simpleMailMessage.setCc("admin@ashuboi.in");
        javaMailSender.send(simpleMailMessage);
        MDC.clear();
    }


    @KafkaListener(topics = "WALLET-UPDATED", groupId = "email")
    public void consumeFromWalletUpdatedTopic(ConsumerRecord payload) throws JsonProcessingException {
        WalletUpdatedPayload walletUpdatedPayload = objectMapper.readValue(payload.value().toString(),WalletUpdatedPayload.class);
        MDC.put("requestId",walletUpdatedPayload.getRequestId());
        LOGGER.info("Getting payload from kafka : {}",payload);
        SimpleMailMessage simpleMailMessage = new SimpleMailMessage();
        simpleMailMessage.setFrom("jbdl.ewallet@gmail.com");
        simpleMailMessage.setSubject(walletUpdatedPayload.getUserName()+"'s Wallet Updated");
        simpleMailMessage.setTo(walletUpdatedPayload.getUserEmail());
        simpleMailMessage.setText("Hi "+walletUpdatedPayload.getUserName()+", Your wallet is updated. New balance is "+walletUpdatedPayload.getBalance());
        simpleMailMessage.setCc("admin.jbdl54@yopmail.com");
        javaMailSender.send(simpleMailMessage);
        MDC.clear();
    }

}