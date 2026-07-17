package com.example.exchange.ledger.repository;

import com.example.exchange.ledger.entity.LedgerEntry;
import com.example.exchange.ledger.entity.LedgerPosting;
import com.example.exchange.ledger.entity.LedgerTransaction;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryLedgerRepository implements LedgerRepository {
    private final AtomicLong transactionSequence = new AtomicLong();
    private final AtomicLong accountSequence = new AtomicLong();
    private final AtomicLong entrySequence = new AtomicLong();
    private final Map<String, Long> ledgerAccountIds = new LinkedHashMap<>();
    private final Map<String, LedgerTransaction> transactionsByKey = new LinkedHashMap<>();

    @Override
    public synchronized Optional<LedgerTransaction> findByIdempotencyKey(String idempotencyKey) {
        return Optional.ofNullable(transactionsByKey.get(idempotencyKey));
    }

    @Override
    public synchronized LedgerTransaction save(
            String referenceType,
            String referenceId,
            String idempotencyKey,
            String description,
            List<LedgerPosting> postings
    ) {
        Instant createdAt = Instant.now();
        long transactionId = transactionSequence.incrementAndGet();
        List<LedgerEntry> entries = postings.stream()
                .map(posting -> new LedgerEntry(
                        entrySequence.incrementAndGet(),
                        ledgerAccountId(posting),
                        posting.ownerAccountId(),
                        posting.asset(),
                        posting.accountType(),
                        posting.direction(),
                        posting.amount().stripTrailingZeros(),
                        createdAt
                ))
                .toList();
        LedgerTransaction transaction = new LedgerTransaction(
                transactionId,
                referenceType,
                referenceId,
                idempotencyKey,
                description,
                createdAt,
                entries
        );
        transactionsByKey.put(idempotencyKey, transaction);
        return transaction;
    }

    @Override
    public synchronized List<LedgerTransaction> findByOwnerAccountId(long accountId, int limit) {
        List<LedgerTransaction> result = new ArrayList<>();
        List<LedgerTransaction> transactions = new ArrayList<>(transactionsByKey.values());
        for (int index = transactions.size() - 1; index >= 0 && result.size() < limit; index--) {
            LedgerTransaction transaction = transactions.get(index);
            boolean belongsToAccount = transaction.entries().stream()
                    .anyMatch(entry -> entry.ownerAccountId() != null && entry.ownerAccountId() == accountId);
            if (belongsToAccount) {
                result.add(transaction);
            }
        }
        return result;
    }

    @Override
    public synchronized void clear() {
        transactionsByKey.clear();
        ledgerAccountIds.clear();
        transactionSequence.set(0);
        accountSequence.set(0);
        entrySequence.set(0);
    }

    private long ledgerAccountId(LedgerPosting posting) {
        String key = posting.ownerAccountId() + ":" + posting.asset() + ":" + posting.accountType();
        return ledgerAccountIds.computeIfAbsent(key, ignored -> accountSequence.incrementAndGet());
    }
}
