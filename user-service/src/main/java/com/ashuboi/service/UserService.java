package com.ashuboi.service;

import com.ashuboi.Entity.User;
import com.ashuboi.UserCreatedPayload;
import com.ashuboi.dto.UserDto;
import com.ashuboi.repository.UserRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;

@Service
public class UserService {
    private static Logger LOGGER= LoggerFactory.getLogger(UserService.class);
    private static String USER_CREATED_TOPIC = "USER_CREATED";

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private UserRepo userRepo;

    public Long createUser(UserDto userDto){
        User user = User.builder()
                .name(userDto.getName())
                .email(userDto.getEmail())
                .email(userDto.getEmail())
                .phone(userDto.getAddress())
                .kycId(userDto.getKycId())
                .build();
        userRepo.save(user);

        UserCreatedPayload userCreatedPayload = new UserCreatedPayload(user.getId(),user.getName(),user.getEmail(),"123");

        ListenableFuture<SendResult<String,Object>> future = (ListenableFuture<SendResult<String, Object>>) kafkaTemplate.send(USER_CREATED_TOPIC, String.valueOf(user.getId()),userCreatedPayload);

        Logger.info("Pushed userCreatedPayLoad to Kafka: {}", future.get());

        return user.getId();
    }
}
