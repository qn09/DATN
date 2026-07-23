package com.example.exchange.deposit.service;

import com.example.exchange.common.AssetCatalog;
import com.example.exchange.deposit.dto.CreateFiatDepositRequest;
import com.example.exchange.deposit.dto.GatewayDepositCallbackRequest;
import com.example.exchange.deposit.entity.FiatDepositRequest;
import com.example.exchange.deposit.entity.FiatDepositCallbackEvent;
import com.example.exchange.deposit.entity.FiatDepositStatus;
import com.example.exchange.deposit.gateway.DomesticTransferGateway;
import com.example.exchange.deposit.gateway.GatewaySubmission;
import com.example.exchange.deposit.repository.FiatDepositRepository;
import com.example.exchange.deposit.security.GatewayCallbackSignature;
import com.example.exchange.wallet.service.WalletService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class FiatDepositService {
    private static final BigDecimal MAX_DEPOSIT_VND = new BigDecimal("1000000000000");

    private final FiatDepositRepository deposits;
    private final DomesticTransferGateway gateway;
    private final GatewayCallbackSignature signatures;
    private final WalletService wallets;

    public FiatDepositService(
            FiatDepositRepository deposits,
            DomesticTransferGateway gateway,
            GatewayCallbackSignature signatures,
            WalletService wallets
    ) {
        this.deposits = deposits;
        this.gateway = gateway;
        this.signatures = signatures;
        this.wallets = wallets;
    }

    @Transactional
    public synchronized FiatDepositRequest create(CreateFiatDepositRequest input, String idempotencyKey) {
        if (input == null || input.accountId() <= 0) {
            throw new IllegalArgumentException("accountId is required");
        }
        String currency = normalizeCurrency(input.currency());
        BigDecimal amount = requireAmount(input.amount());
        String clientKey = input.accountId() + ":" + requireIdempotencyKey(idempotencyKey);

        return deposits.findByClientRequestKey(clientKey)
                .map(existing -> {
                    if (!existing.currency().equals(currency) || existing.amount().compareTo(amount) != 0) {
                        throw new IllegalArgumentException(
                                "Idempotency-Key was already used with different fiat deposit data"
                        );
                    }
                    return existing;
                })
                .orElseGet(() -> deposits.create(
                        UUID.randomUUID().toString(),
                        input.accountId(),
                        currency,
                        amount,
                        gateway.name(),
                        clientKey
                ));
    }

    @Transactional
    public synchronized FiatDepositRequest submit(String requestId) {
        FiatDepositRequest request = requireForUpdate(requestId);
        if (request.status() == FiatDepositStatus.PROCESSING || request.status() == FiatDepositStatus.SUCCESS) {
            return request;
        }
        if (request.status() != FiatDepositStatus.NEW) {
            throw new IllegalArgumentException("only NEW fiat deposit can be submitted");
        }

        GatewaySubmission submission = gateway.submit(request);
        if (submission == null || submission.gatewayReference() == null
                || submission.gatewayReference().isBlank()) {
            throw new IllegalStateException("domestic transfer gateway did not return a reference");
        }
        return deposits.markProcessing(request.requestId(), submission.gatewayReference().trim());
    }

    @Transactional
    public synchronized FiatDepositRequest processCallback(
            GatewayDepositCallbackRequest callback,
            String signature
    ) {
        signatures.requireValid(signature, callback);
        FiatDepositRequest request = requireForUpdate(callback.requestId());
        String eventId = callback.eventId().trim();
        String normalizedSignature = signature.trim().toLowerCase(Locale.ROOT);
        FiatDepositCallbackEvent existingEvent = deposits.findCallbackEvent(eventId).orElse(null);
        if (existingEvent != null) {
            if (!existingEvent.requestId().equals(request.requestId())
                    || !existingEvent.signature().equals(normalizedSignature)) {
                throw new IllegalArgumentException("gateway callback event id was reused with different data");
            }
            return request;
        }
        validateCallback(request, callback);

        FiatDepositStatus callbackStatus = callbackStatus(callback.status());
        if (request.status() == callbackStatus && request.status().isTerminal()) {
            deposits.recordCallbackEvent(eventId, request.requestId(), normalizedSignature);
            return request;
        }
        if (request.status() != FiatDepositStatus.PROCESSING) {
            throw new IllegalArgumentException("fiat deposit callback requires PROCESSING status");
        }

        if (callbackStatus == FiatDepositStatus.SUCCESS) {
            wallets.creditFiatDeposit(
                    request.accountId(), request.currency(), request.amount(), request.requestId()
            );
            FiatDepositRequest completed = deposits.markSuccess(request.requestId());
            deposits.recordCallbackEvent(eventId, request.requestId(), normalizedSignature);
            return completed;
        }
        FiatDepositRequest completed = deposits.markFailed(
                request.requestId(), failureReason(callback.failureReason())
        );
        deposits.recordCallbackEvent(eventId, request.requestId(), normalizedSignature);
        return completed;
    }

    public FiatDepositRequest require(String requestId) {
        if (requestId == null || requestId.isBlank()) {
            throw new IllegalArgumentException("requestId is required");
        }
        return deposits.findByRequestId(requestId.trim())
                .orElseThrow(() -> new IllegalArgumentException("fiat deposit not found: " + requestId));
    }

    public List<FiatDepositRequest> history(long accountId, int limit) {
        return deposits.findByAccountId(accountId, Math.min(Math.max(limit, 1), 200));
    }

    private FiatDepositRequest requireForUpdate(String requestId) {
        if (requestId == null || requestId.isBlank()) {
            throw new IllegalArgumentException("requestId is required");
        }
        return deposits.findByRequestIdForUpdate(requestId.trim())
                .orElseThrow(() -> new IllegalArgumentException("fiat deposit not found: " + requestId));
    }

    private void validateCallback(FiatDepositRequest request, GatewayDepositCallbackRequest callback) {
        if (!request.gatewayReference().equals(value(callback.gatewayReference()))) {
            throw new IllegalArgumentException("gateway reference does not match fiat deposit");
        }
        if (!request.currency().equals(normalizeCurrency(callback.currency()))) {
            throw new IllegalArgumentException("gateway callback currency does not match fiat deposit");
        }
        if (callback.amount() == null || request.amount().compareTo(callback.amount()) != 0) {
            throw new IllegalArgumentException("gateway callback amount does not match fiat deposit");
        }
    }

    private FiatDepositStatus callbackStatus(String status) {
        try {
            FiatDepositStatus value = FiatDepositStatus.valueOf(value(status).toUpperCase(Locale.ROOT));
            if (value != FiatDepositStatus.SUCCESS && value != FiatDepositStatus.FAILED) {
                throw new IllegalArgumentException("gateway callback status must be SUCCESS or FAILED");
            }
            return value;
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("gateway callback status must be SUCCESS or FAILED");
        }
    }

    private String normalizeCurrency(String currency) {
        String normalized = value(currency).toUpperCase(Locale.ROOT);
        if (!AssetCatalog.FIAT_ASSET.equals(normalized)) {
            throw new IllegalArgumentException("fiat deposit currency must be " + AssetCatalog.FIAT_ASSET);
        }
        return normalized;
    }

    private BigDecimal requireAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        BigDecimal normalized = amount.stripTrailingZeros();
        if (normalized.scale() > 0) {
            throw new IllegalArgumentException("VND amount must be a whole number");
        }
        if (normalized.compareTo(MAX_DEPOSIT_VND) > 0) {
            throw new IllegalArgumentException("VND amount exceeds deposit limit");
        }
        return normalized;
    }

    private String requireIdempotencyKey(String idempotencyKey) {
        String value = value(idempotencyKey);
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Idempotency-Key is required");
        }
        if (value.length() > 128) {
            throw new IllegalArgumentException("Idempotency-Key must not exceed 128 characters");
        }
        return value;
    }

    private String failureReason(String reason) {
        String value = value(reason);
        return value.isEmpty() ? "gateway rejected deposit" : value.substring(0, Math.min(value.length(), 500));
    }

    private String value(String value) {
        return value == null ? "" : value.trim();
    }
}
