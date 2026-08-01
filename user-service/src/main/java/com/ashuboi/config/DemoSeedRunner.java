package com.ashuboi.config;

import com.ashuboi.dto.UserDto;
import com.ashuboi.repository.UserRepo;
import com.ashuboi.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DemoSeedRunner implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(DemoSeedRunner.class);

    private final boolean seedDemo;
    private final UserRepo userRepo;
    private final UserService userService;

    public DemoSeedRunner(
            @Value("${app.seed-demo:false}") boolean seedDemo,
            UserRepo userRepo,
            UserService userService) {
        this.seedDemo = seedDemo;
        this.userRepo = userRepo;
        this.userService = userService;
    }

    @Override
    public void run(String... args) {
        if (!seedDemo) {
            return;
        }
        seedIfMissing("Alice Demo", "alice@demo.local", "password123", "9000000001", "KYC-ALICE");
        seedIfMissing("Bob Demo", "bob@demo.local", "password123", "9000000002", "KYC-BOB");
    }

    private void seedIfMissing(String name, String email, String password, String phone, String kycId) {
        if (userRepo.existsByEmail(email)) {
            LOGGER.info("Demo user already present: {}", email);
            return;
        }
        try {
            userService.register(UserDto.builder()
                    .name(name)
                    .email(email)
                    .password(password)
                    .phone(phone)
                    .kycId(kycId)
                    .address("Demo City")
                    .build());
            LOGGER.info("Seeded demo user {}", email);
        } catch (Exception e) {
            LOGGER.warn("Could not seed {}: {}", email, e.getMessage());
        }
    }
}
