package com.ashuboi.service;

import com.ashuboi.Entity.User;
import com.ashuboi.dto.AuthResponse;
import com.ashuboi.dto.LoginRequest;
import com.ashuboi.dto.UserDto;
import com.ashuboi.exception.ResourceNotFoundException;
import com.ashuboi.payload.UserCreatedPayload;
import com.ashuboi.repository.UserRepo;
import com.ashuboi.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Service
public class UserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);
    private static final String USER_CREATED_TOPIC = "USER-CREATED";

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final UserRepo userRepo;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserService(KafkaTemplate<String, Object> kafkaTemplate, UserRepo userRepo, JwtService jwtService) {
        this.kafkaTemplate = kafkaTemplate;
        this.userRepo = userRepo;
        this.jwtService = jwtService;
    }

    public AuthResponse register(UserDto userDto) throws ExecutionException, InterruptedException {
        if (userRepo.existsByEmail(userDto.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }

        User user = User.builder()
                .name(userDto.getName())
                .email(userDto.getEmail())
                .passwordHash(passwordEncoder.encode(userDto.getPassword()))
                .phone(userDto.getPhone())
                .address(userDto.getAddress())
                .kycId(userDto.getKycId())
                .build();
        userRepo.save(user);

        UserCreatedPayload payload = UserCreatedPayload.builder()
                .userId(user.getId())
                .userName(user.getName())
                .userEmail(user.getEmail())
                .requestId(MDC.get("requestId"))
                .build();

        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(USER_CREATED_TOPIC, user.getId().toString(), payload);
        LOGGER.info("Published USER-CREATED for userId={}", user.getId());
        future.get();

        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getName());
        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepo.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }
        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getName());
        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }

    public User getById(Long id) {
        return userRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }
}
