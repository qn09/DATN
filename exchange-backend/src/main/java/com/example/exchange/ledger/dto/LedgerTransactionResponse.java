package com.example.exchange.ledger.dto;

import java.time.Instant;
import java.util.List;

public record LedgerTransactionResponse(
        long id,
        String referenceType,
        String referenceId,
        String description,
        Instant createdAt,
        List<LedgerEntryResponse> entries
) {
}
