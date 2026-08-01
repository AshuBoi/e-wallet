package com.ashuboi.security;

public record AuthenticatedUser(Long userId, String email, String name) {
}
