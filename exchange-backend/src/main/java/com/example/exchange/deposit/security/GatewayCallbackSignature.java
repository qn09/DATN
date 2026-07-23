package com.example.exchange.deposit.security;

import com.example.exchange.deposit.dto.GatewayDepositCallbackRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.util.HexFormat;

@Component
public class GatewayCallbackSignature {
    private final byte[] secret;
    private final long maxAgeSeconds;
    private final Clock clock;

    @Autowired
    public GatewayCallbackSignature(
            @Value("${app.fiat-deposit.webhook-secret}") String secret,
            @Value("${app.fiat-deposit.callback-max-age-seconds:300}") long maxAgeSeconds
    ) {
        this(secret, maxAgeSeconds, Clock.systemUTC());
    }

    public GatewayCallbackSignature(String secret) {
        this(secret, 300, Clock.systemUTC());
    }

    public GatewayCallbackSignature(String secret, long maxAgeSeconds, Clock clock) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("fiat gateway webhook secret is required");
        }
        if (maxAgeSeconds <= 0) {
            throw new IllegalArgumentException("fiat gateway callback max age must be positive");
        }
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.maxAgeSeconds = maxAgeSeconds;
        this.clock = clock;
    }

    public void requireValid(String signature, GatewayDepositCallbackRequest callback) {
        if (callback == null) {
            throw new IllegalArgumentException("gateway callback is required");
        }
        if (signature == null || signature.isBlank()) {
            throw new IllegalArgumentException("X-Gateway-Signature is required");
        }
        requireEventMetadata(callback);
        byte[] expected = sign(callback).getBytes(StandardCharsets.UTF_8);
        byte[] actual = signature.trim().toLowerCase().getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expected, actual)) {
            throw new IllegalArgumentException("invalid gateway callback signature");
        }
    }

    public String sign(GatewayDepositCallbackRequest callback) {
        String canonical = String.join("|",
                value(callback.eventId()),
                callback.occurredAt() == null ? "" : callback.occurredAt().toString(),
                value(callback.requestId()),
                value(callback.gatewayReference()),
                value(callback.status()).toUpperCase(),
                value(callback.currency()).toUpperCase(),
                callback.amount() == null ? "" : callback.amount().stripTrailingZeros().toPlainString(),
                value(callback.failureReason())
        );
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.GeneralSecurityException exception) {
            throw new IllegalStateException("cannot sign gateway callback", exception);
        }
    }

    private void requireEventMetadata(GatewayDepositCallbackRequest callback) {
        String eventId = value(callback.eventId());
        if (eventId.isEmpty()) {
            throw new IllegalArgumentException("gateway callback eventId is required");
        }
        if (eventId.length() > 128) {
            throw new IllegalArgumentException("gateway callback eventId must not exceed 128 characters");
        }
        if (callback.occurredAt() == null) {
            throw new IllegalArgumentException("gateway callback occurredAt is required");
        }
        long ageSeconds = Math.abs(Duration.between(callback.occurredAt(), clock.instant()).getSeconds());
        if (ageSeconds > maxAgeSeconds) {
            throw new IllegalArgumentException("gateway callback timestamp is outside the allowed window");
        }
    }

    private String value(String value) {
        return value == null ? "" : value.trim();
    }
}
