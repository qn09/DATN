package com.example.exchangeadmin.auth;

public record AdminPrincipal(long id, String username, String passwordHash, String role) {
    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }
}
