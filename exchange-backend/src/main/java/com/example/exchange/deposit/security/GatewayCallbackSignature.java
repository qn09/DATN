package com.example.exchange.deposit.security;

import com.example.exchange.deposit.dto.GatewayDepositCallbackRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Component
public class GatewayCallbackSignature {
    private final byte[] secret;

    public GatewayCallbackSignature(@Value("${app.fiat-deposit.webhook-secret}") String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("fiat gateway webhook secret is required");
        }
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    public void requireValid(String signature, GatewayDepositCallbackRequest callback) {
        if (signature == null || signature.isBlank()) {
            throw new IllegalArgumentException("X-Gateway-Signature is required");
        }
        byte[] expected = sign(callback).getBytes(StandardCharsets.UTF_8);
        byte[] actual = signature.trim().toLowerCase().getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expected, actual)) {
            throw new IllegalArgumentException("invalid gateway callback signature");
        }
    }

    public String sign(GatewayDepositCallbackRequest callback) {
        String canonical = String.join("|",
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

    private String value(String value) {
        return value == null ? "" : value.trim();
    }
}
