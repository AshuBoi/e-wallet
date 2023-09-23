package com.ashuboi.payload;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class UserCreatedPayload {
    private Long userId;
    private String userName;
    private String userEmail;
    private String requestId;

}

