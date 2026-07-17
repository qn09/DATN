package com.example.exchangeadmin.auth;

public record AdminAccountResponse(long id, String username, String role) {
    public static AdminAccountResponse from(AdminPrincipal account) {
        return new AdminAccountResponse(account.id(), account.username(), account.role());
    }
}
