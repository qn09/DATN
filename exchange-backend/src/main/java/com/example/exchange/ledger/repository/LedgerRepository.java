package com.example.exchange.ledger.repository;

import com.example.exchange.ledger.entity.LedgerPosting;
import com.example.exchange.ledger.entity.LedgerTransaction;

import java.util.List;
import java.util.Optional;

public interface LedgerRepository {
    Optional<LedgerTransaction> findByIdempotencyKey(String idempotencyKey);

    LedgerTransaction save(
            String referenceType,
            String referenceId,
            String idempotencyKey,
            String description,
            List<LedgerPosting> postings
    );

    List<LedgerTransaction> findByOwnerAccountId(long accountId, int limit);

    void clear();
}
