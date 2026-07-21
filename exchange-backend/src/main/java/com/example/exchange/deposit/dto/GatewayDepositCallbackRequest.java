package com.example.exchange.deposit.dto;

import java.math.BigDecimal;

public record GatewayDepositCallbackRequest(
        String requestId,
        String gatewayReference,
        String status,
        String currency,
        BigDecimal amount,
        String failureReason
) {
}
