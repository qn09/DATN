package com.example.exchange.deposit.dto;

import com.example.exchange.deposit.entity.FiatDepositRequest;
import com.example.exchange.deposit.entity.FiatDepositStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record FiatDepositResponse(
        String requestId,
        long accountId,
        String currency,
        BigDecimal amount,
        FiatDepositStatus status,
        String gateway,
        String gatewayReference,
        String failureReason,
        Instant createdAt,
        Instant processingAt,
        Instant completedAt,
        Instant updatedAt
) {
    public static FiatDepositResponse from(FiatDepositRequest request) {
        return new FiatDepositResponse(
                request.requestId(),
                request.accountId(),
                request.currency(),
                request.amount(),
                request.status(),
                request.gateway(),
                request.gatewayReference(),
                request.failureReason(),
                request.createdAt(),
                request.processingAt(),
                request.completedAt(),
                request.updatedAt()
        );
    }
}
