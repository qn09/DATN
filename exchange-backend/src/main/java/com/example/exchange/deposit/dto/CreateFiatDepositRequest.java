package com.example.exchange.deposit.dto;

import java.math.BigDecimal;

public record CreateFiatDepositRequest(long accountId, String currency, BigDecimal amount) {
}
