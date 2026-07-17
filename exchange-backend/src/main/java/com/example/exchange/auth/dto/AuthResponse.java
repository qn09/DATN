package com.example.exchange.auth.dto;

import com.example.exchange.auth.entity.Account;

public record AuthResponse(String token, Account account) {
}
