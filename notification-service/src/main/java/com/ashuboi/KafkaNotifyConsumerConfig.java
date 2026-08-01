package com.ashuboi;

import com.ashuboi.payload.UserCreatedPayload;
import com.ashuboi.payload.WalletUpdatedPayload;
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
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaNotifyConsumerConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaNotifyConsumerConfig.class);

    private final JavaMailSender javaMailSender;
    private final ObjectMapper objectMapper;
    private final String mailFrom;

    @Value("${kafka.bootstrapAddress}")
    private String bootStrapServers;

    public KafkaNotifyConsumerConfig(
            JavaMailSender javaMailSender,
            ObjectMapper objectMapper,
            @Value("${mail.from:wallet@ewallet.local}") String mailFrom) {
        this.javaMailSender = javaMailSender;
        this.objectMapper = objectMapper;
        this.mailFrom = mailFrom;
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

    @KafkaListener(topics = "USER-CREATED", groupId = "email")
    public void consumeFromUserCreatedTopic(ConsumerRecord<?, ?> payload) throws Exception {
        UserCreatedPayload userCreatedPayload =
                objectMapper.readValue(payload.value().toString(), UserCreatedPayload.class);
        MDC.put("requestId", userCreatedPayload.getRequestId());
        LOGGER.info("Sending welcome email to {}", userCreatedPayload.getUserEmail());

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setSubject("Welcome " + userCreatedPayload.getUserName() + "!");
        message.setTo(userCreatedPayload.getUserEmail());
        message.setText("Hi " + userCreatedPayload.getUserName()
                + ", welcome to the e-wallet. Your wallet will be provisioned shortly.");
        javaMailSender.send(message);
        MDC.clear();
    }

    @KafkaListener(topics = "WALLET-UPDATED", groupId = "email")
    public void consumeFromWalletUpdatedTopic(ConsumerRecord<?, ?> payload) throws Exception {
        WalletUpdatedPayload walletUpdatedPayload =
                objectMapper.readValue(payload.value().toString(), WalletUpdatedPayload.class);
        MDC.put("requestId", walletUpdatedPayload.getRequestId());
        LOGGER.info("Sending balance email to {}", walletUpdatedPayload.getUserEmail());

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setSubject(walletUpdatedPayload.getUserName() + "'s Wallet Updated");
        message.setTo(walletUpdatedPayload.getUserEmail());
        message.setText("Hi " + walletUpdatedPayload.getUserName()
                + ", your wallet was updated. New balance: " + walletUpdatedPayload.getBalance());
        javaMailSender.send(message);
        MDC.clear();
    }
}
