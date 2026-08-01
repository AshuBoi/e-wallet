package com.ashuboi.Controller;

import com.ashuboi.Entity.User;
import com.ashuboi.dto.AuthResponse;
import com.ashuboi.dto.LoginRequest;
import com.ashuboi.dto.UserDto;
import com.ashuboi.security.AuthContext;
import com.ashuboi.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/user-service")
public class UserController {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/auth/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody UserDto userDto)
            throws ExecutionException, InterruptedException {
        LOGGER.info("Register request for email={}", userDto.getEmail());
        return ResponseEntity.ok(userService.register(userDto));
    }

    @PostMapping("/auth/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        LOGGER.info("Login request for email={}", request.getEmail());
        return ResponseEntity.ok(userService.login(request));
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me() {
        User user = userService.getById(AuthContext.requireUserId());
        return ResponseEntity.ok(Map.of(
                "userId", user.getId(),
                "name", user.getName(),
                "email", user.getEmail(),
                "phone", user.getPhone()
        ));
    }

    /** Backward-compatible alias for older clients. */
    @PostMapping("/user")
    public ResponseEntity<AuthResponse> createUser(@Valid @RequestBody UserDto userDto)
            throws ExecutionException, InterruptedException {
        return register(userDto);
    }
}
