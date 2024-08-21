package com.ashuboi.Controller;

import com.ashuboi.dto.UserDto;
import com.ashuboi.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/user-service")
public class
UserController {

    private static Logger LOGGER = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    @PostMapping("/user")
    public ResponseEntity<Long> createUser(@RequestBody UserDto userDto) throws ExecutionException, InterruptedException {
        LOGGER.info("Creating User : {}",userDto);
        Long userId = userService.createUser(userDto);
        return ResponseEntity.ok(userId);
    }
}

