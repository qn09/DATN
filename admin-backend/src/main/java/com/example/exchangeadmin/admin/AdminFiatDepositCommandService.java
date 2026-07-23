package com.example.exchangeadmin.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;

@Service
public class AdminFiatDepositCommandService {
    private final AdminQueryService queries;
    private final AdminGatewayCallbackSignature signatures;
    private final RestClient exchangeApi;
    private final Clock clock;

    @Autowired
    public AdminFiatDepositCommandService(
            AdminQueryService queries,
            AdminGatewayCallbackSignature signatures,
            RestClient.Builder restClientBuilder,
            @Value("${app.exchange-api.base-url:http://localhost:8080}") String exchangeApiBaseUrl
    ) {
        this(queries, signatures, restClientBuilder.baseUrl(exchangeApiBaseUrl).build(), Clock.systemUTC());
    }

    AdminFiatDepositCommandService(
            AdminQueryService queries,
            AdminGatewayCallbackSignature signatures,
            RestClient exchangeApi,
            Clock clock
    ) {
        this.queries = queries;
        this.signatures = signatures;
        this.exchangeApi = exchangeApi;
        this.clock = clock;
    }

    public AdminFiatDepositView decide(
            String requestId,
            String idempotencyKey,
            AdminFiatDepositDecisionRequest input
    ) {
        AdminFiatDepositView deposit = queries.requireFiatDeposit(requestId);
        String status = requireDecision(input);
        if (deposit.status().equals(status)) {
            return deposit;
        }
        if (!"PROCESSING".equals(deposit.status())) {
            throw new IllegalArgumentException("only PROCESSING fiat deposit can be approved or rejected");
        }

        String eventId = requireIdempotencyKey(idempotencyKey);
        String failureReason = "FAILED".equals(status)
                ? failureReason(input.failureReason())
                : null;
        ExchangeFiatDepositCallback callback = new ExchangeFiatDepositCallback(
                eventId,
                Instant.now(clock),
                deposit.requestId(),
                deposit.gatewayReference(),
                status,
                deposit.currency(),
                deposit.amount(),
                failureReason
        );

        try {
            exchangeApi.post()
                    .uri("/api/fiat-deposits/gateway/callback")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Gateway-Signature", signatures.sign(callback))
                    .body(callback)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException exception) {
            throw new AdminDepositGatewayException(
                    "exchange backend rejected the fiat deposit decision", exception
            );
        }
        return queries.requireFiatDeposit(requestId);
    }

    private String requireDecision(AdminFiatDepositDecisionRequest input) {
        if (input == null || input.status() == null) {
            throw new IllegalArgumentException("status is required");
        }
        String status = input.status().trim().toUpperCase(Locale.ROOT);
        if (!"SUCCESS".equals(status) && !"FAILED".equals(status)) {
            throw new IllegalArgumentException("status must be SUCCESS or FAILED");
        }
        return status;
    }

    private String requireIdempotencyKey(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Idempotency-Key is required");
        }
        String normalized = value.trim();
        if (normalized.length() > 128) {
            throw new IllegalArgumentException("Idempotency-Key must not exceed 128 characters");
        }
        return normalized;
    }

    private String failureReason(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("failureReason is required when rejecting a deposit");
        }
        String normalized = value.trim();
        return normalized.substring(0, Math.min(normalized.length(), 500));
    }
}
