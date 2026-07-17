package com.example.exchange.auth.service;

import com.example.exchange.auth.dto.AuthResponse;
import com.example.exchange.auth.dto.LoginRequest;
import com.example.exchange.auth.dto.RegisterRequest;
import com.example.exchange.auth.entity.Account;
import com.example.exchange.auth.entity.Role;
import com.example.exchange.auth.repository.AccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final AccountRepository accounts;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwt;

    public AuthService(AccountRepository accounts, PasswordEncoder passwordEncoder, JwtService jwt) {
        this.accounts = accounts;
        this.passwordEncoder = passwordEncoder;
        this.jwt = jwt;
    }

    public AuthResponse register(RegisterRequest request) {
        String username = requireText(request.username(), "username");
        String password = requireText(request.password(), "password");
        if (password.length() < 6) {
            throw new IllegalArgumentException("password must be at least 6 characters");
        }
        accounts.findByUsername(username).ifPresent(account -> {
            throw new IllegalArgumentException("username already exists");
        });

        Account account = accounts.create(username, passwordEncoder.encode(password), Role.USER);
        return new AuthResponse(jwt.createToken(account), account);
    }

    public AuthResponse login(LoginRequest request) {
        String username = requireText(request.username(), "username");
        String password = requireText(request.password(), "password");
        Account account = accounts.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("invalid username or password"));

        if (account.passwordHash() == null || !passwordEncoder.matches(password, account.passwordHash())) {
            throw new IllegalArgumentException("invalid username or password");
        }

        return new AuthResponse(jwt.createToken(account), account);
    }

    private String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
