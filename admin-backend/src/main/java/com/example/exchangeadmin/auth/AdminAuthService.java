package com.example.exchangeadmin.auth;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AdminAuthService {
    private final AdminAccountRepository accounts;
    private final PasswordEncoder passwordEncoder;
    private final AdminJwtService jwt;

    public AdminAuthService(
            AdminAccountRepository accounts,
            PasswordEncoder passwordEncoder,
            AdminJwtService jwt
    ) {
        this.accounts = accounts;
        this.passwordEncoder = passwordEncoder;
        this.jwt = jwt;
    }

    public AdminAuthResponse login(AdminLoginRequest request) {
        String username = requireText(request.username(), "username");
        String password = requireText(request.password(), "password");
        AdminPrincipal account = accounts.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("invalid username or password"));

        if (!account.isAdmin()) {
            throw new IllegalArgumentException("admin role required");
        }
        if (account.passwordHash() == null || !passwordEncoder.matches(password, account.passwordHash())) {
            throw new IllegalArgumentException("invalid username or password");
        }
        return new AdminAuthResponse(jwt.createToken(account), AdminAccountResponse.from(account));
    }

    private String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
