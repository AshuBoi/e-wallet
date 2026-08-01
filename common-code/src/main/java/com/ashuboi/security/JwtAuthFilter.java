package com.ashuboi.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ashuboi.exception.ApiError;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

@Component
@Order(2)
public class JwtAuthFilter extends HttpFilter {

    private static final List<String> PUBLIC_SUFFIXES = List.of(
            "/auth/register",
            "/auth/login",
            "/actuator/health",
            "/v3/api-docs",
            "/swagger-ui",
            "/webhooks/",
            "/mock-pg/",
            "/hello"
    );

    private final JwtService jwtService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        String path = request.getRequestURI();
        if ("OPTIONS".equalsIgnoreCase(request.getMethod()) || isPublic(path)) {
            chain.doFilter(request, response);
            return;
        }

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            writeUnauthorized(response, "Missing or invalid Authorization header");
            return;
        }

        try {
            AuthenticatedUser user = jwtService.parseToken(header.substring(7));
            AuthContext.set(user);
            chain.doFilter(request, response);
        } catch (Exception ex) {
            writeUnauthorized(response, "Invalid or expired token");
        } finally {
            AuthContext.clear();
        }
    }

    private boolean isPublic(String path) {
        return PUBLIC_SUFFIXES.stream().anyMatch(path::contains);
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiError error = new ApiError(Instant.now().toString(), 401, "Unauthorized", message, null);
        response.getWriter().write(objectMapper.writeValueAsString(error));
    }
}
