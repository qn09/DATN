package com.example.exchange.auth.controller;

import com.example.exchange.auth.dto.AuthResponse;
import com.example.exchange.auth.dto.LoginRequest;
import com.example.exchange.auth.dto.RegisterRequest;
import com.example.exchange.auth.service.AuthService;
import com.example.exchange.auth.service.LoginRateLimiterService;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;
    private final LoginRateLimiterService loginRateLimiter;

    public AuthController(AuthService auth, LoginRateLimiterService loginRateLimiter) {
        this.auth = auth;
        this.loginRateLimiter = loginRateLimiter;
    }

    @PostMapping("/register")
    public AuthResponse register(@RequestBody RegisterRequest request) {
        return auth.register(request);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        ConsumptionProbe probe = loginRateLimiter.tryConsume(clientIp(servletRequest), request.username());
        if (!probe.isConsumed()) {
            long retryAfterSeconds = TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill());
            return ResponseEntity
                    .status(HttpStatus.TOO_MANY_REQUESTS)
                    .header("Retry-After", String.valueOf(Math.max(1, retryAfterSeconds)))
                    .body(Map.of(
                            "error", "too many login attempts",
                            "limit", "5 requests per 1 minute"
                    ));
        }

        return ResponseEntity.ok(auth.login(request));
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
