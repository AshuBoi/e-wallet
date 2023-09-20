package com.ashuboi.Controller;

import com.ashuboi.dto.UserDto;
import com.ashuboi.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user-service")
public class UserController {

    @Autowired
    private UserService userService;
    @PostMapping("/user")
    public ResponseEntity<Long> createUser(@RequestBody UserDto userDto)
    {
        Long userId = userService.createUser(userDto);
        return ResponseEntity.ok(userId);
    }
}
