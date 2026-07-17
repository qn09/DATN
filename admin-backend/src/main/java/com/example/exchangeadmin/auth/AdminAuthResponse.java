package com.example.exchangeadmin.auth;

public record AdminAuthResponse(String token, AdminAccountResponse account) {
}
