package com.example.exchange.deposit.repository;

import com.example.exchange.deposit.entity.FiatDepositRequest;
import com.example.exchange.deposit.entity.FiatDepositStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryFiatDepositRepository implements FiatDepositRepository {
    private final AtomicLong ids = new AtomicLong();
    private final Map<String, FiatDepositRequest> requests = new LinkedHashMap<>();

    @Override
    public synchronized FiatDepositRequest create(
            String requestId,
            long accountId,
            String currency,
            BigDecimal amount,
            String gateway,
            String clientRequestKey
    ) {
        Instant now = Instant.now();
        FiatDepositRequest request = new FiatDepositRequest(
                ids.incrementAndGet(), requestId, accountId, currency, amount.stripTrailingZeros(),
                FiatDepositStatus.NEW, gateway, null, clientRequestKey, null,
                now, null, null, now
        );
        requests.put(requestId, request);
        return request;
    }

    @Override
    public synchronized Optional<FiatDepositRequest> findByRequestId(String requestId) {
        return Optional.ofNullable(requests.get(requestId));
    }

    @Override
    public synchronized Optional<FiatDepositRequest> findByRequestIdForUpdate(String requestId) {
        return findByRequestId(requestId);
    }

    @Override
    public synchronized Optional<FiatDepositRequest> findByClientRequestKey(String clientRequestKey) {
        return requests.values().stream()
                .filter(request -> request.clientRequestKey().equals(clientRequestKey))
                .findFirst();
    }

    @Override
    public synchronized List<FiatDepositRequest> findByAccountId(long accountId, int limit) {
        return requests.values().stream()
                .filter(request -> request.accountId() == accountId)
                .sorted(Comparator.comparingLong(FiatDepositRequest::id).reversed())
                .limit(limit)
                .toList();
    }

    @Override
    public synchronized FiatDepositRequest markProcessing(String requestId, String gatewayReference) {
        FiatDepositRequest current = requireStatus(requestId, FiatDepositStatus.NEW);
        return save(copy(current, FiatDepositStatus.PROCESSING, gatewayReference, null, Instant.now(), null));
    }

    @Override
    public synchronized FiatDepositRequest markSuccess(String requestId) {
        FiatDepositRequest current = requireStatus(requestId, FiatDepositStatus.PROCESSING);
        return save(copy(current, FiatDepositStatus.SUCCESS, current.gatewayReference(), null,
                current.processingAt(), Instant.now()));
    }

    @Override
    public synchronized FiatDepositRequest markFailed(String requestId, String failureReason) {
        FiatDepositRequest current = requireStatus(requestId, FiatDepositStatus.PROCESSING);
        return save(copy(current, FiatDepositStatus.FAILED, current.gatewayReference(), failureReason,
                current.processingAt(), Instant.now()));
    }

    private FiatDepositRequest requireStatus(String requestId, FiatDepositStatus status) {
        FiatDepositRequest request = requests.get(requestId);
        if (request == null || request.status() != status) {
            throw new IllegalArgumentException("fiat deposit " + requestId + " is not in " + status + " status");
        }
        return request;
    }

    private FiatDepositRequest save(FiatDepositRequest request) {
        requests.put(request.requestId(), request);
        return request;
    }

    private FiatDepositRequest copy(
            FiatDepositRequest current,
            FiatDepositStatus status,
            String gatewayReference,
            String failureReason,
            Instant processingAt,
            Instant completedAt
    ) {
        return new FiatDepositRequest(
                current.id(), current.requestId(), current.accountId(), current.currency(), current.amount(),
                status, current.gateway(), gatewayReference, current.clientRequestKey(), failureReason,
                current.createdAt(), processingAt, completedAt, Instant.now()
        );
    }
}
