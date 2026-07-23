package com.example.exchangeadmin.admin;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

@Component
public class AdminGatewayCallbackSignature {
    private final byte[] secret;

    public AdminGatewayCallbackSignature(
            @Value("${app.fiat-deposit.webhook-secret}") String secret
    ) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("fiat gateway webhook secret is required");
        }
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    public String sign(ExchangeFiatDepositCallback callback) {
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
            throw new IllegalStateException("cannot sign fiat deposit callback", exception);
        }
    }

    private String value(String value) {
        return value == null ? "" : value.trim();
    }
}
