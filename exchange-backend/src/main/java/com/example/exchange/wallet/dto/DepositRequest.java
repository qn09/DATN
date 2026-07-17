package com.example.exchange.wallet.dto;

import java.math.BigDecimal;

public record DepositRequest(String asset, BigDecimal amount) {
}
