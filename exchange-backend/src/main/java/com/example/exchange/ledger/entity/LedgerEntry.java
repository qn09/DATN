package com.example.exchange.ledger.entity;

import java.math.BigDecimal;
import java.time.Instant;

public record LedgerEntry(
        long id,
        long ledgerAccountId,
        Long ownerAccountId,
        String asset,
        LedgerAccountType accountType,
        EntryDirection direction,
        BigDecimal amount,
        Instant createdAt
) {
}
