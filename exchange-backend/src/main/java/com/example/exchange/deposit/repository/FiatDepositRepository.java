package com.example.exchange.deposit.repository;

import com.example.exchange.deposit.entity.FiatDepositRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface FiatDepositRepository {
    FiatDepositRequest create(
            String requestId,
            long accountId,
            String currency,
            BigDecimal amount,
            String gateway,
            String clientRequestKey
    );

    Optional<FiatDepositRequest> findByRequestId(String requestId);

    Optional<FiatDepositRequest> findByRequestIdForUpdate(String requestId);

    Optional<FiatDepositRequest> findByClientRequestKey(String clientRequestKey);

    List<FiatDepositRequest> findByAccountId(long accountId, int limit);

    FiatDepositRequest markProcessing(String requestId, String gatewayReference);

    FiatDepositRequest markSuccess(String requestId);

    FiatDepositRequest markFailed(String requestId, String failureReason);
}
