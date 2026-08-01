package com.ashuboi.security;

public final class AuthContext {
    private static final ThreadLocal<AuthenticatedUser> CURRENT = new ThreadLocal<>();

    private AuthContext() {
    }

    public static void set(AuthenticatedUser user) {
        CURRENT.set(user);
    }

    public static AuthenticatedUser get() {
        return CURRENT.get();
    }

    public static Long requireUserId() {
        AuthenticatedUser user = CURRENT.get();
        if (user == null) {
            throw new IllegalStateException("Authenticated user required");
        }
        return user.userId();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
