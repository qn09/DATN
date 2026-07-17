package com.example.exchange.ledger.entity;

import java.time.Instant;
import java.util.List;

public record LedgerTransaction(
        long id,
        String referenceType,
        String referenceId,
        String idempotencyKey,
        String description,
        Instant createdAt,
        List<LedgerEntry> entries
) {
}
