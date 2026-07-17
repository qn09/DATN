package com.example.exchange.ledger.service;

import com.example.exchange.common.AssetCatalog;
import com.example.exchange.ledger.entity.EntryDirection;
import com.example.exchange.ledger.entity.LedgerEntry;
import com.example.exchange.ledger.entity.LedgerPosting;
import com.example.exchange.ledger.entity.LedgerTransaction;
import com.example.exchange.ledger.repository.InMemoryLedgerRepository;
import com.example.exchange.ledger.repository.LedgerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class LedgerService {
    private final LedgerRepository ledger;

    public LedgerService() {
        this(new InMemoryLedgerRepository());
    }

    @Autowired
    public LedgerService(LedgerRepository ledger) {
        this.ledger = ledger;
    }

    @Transactional
    public synchronized LedgerPostResult post(
            String referenceType,
            String referenceId,
            String idempotencyKey,
            String description,
            List<LedgerPosting> postings
    ) {
        requireText(referenceType, "referenceType");
        requireText(referenceId, "referenceId");
        requireText(idempotencyKey, "idempotencyKey");
        requireText(description, "description");
        List<LedgerPosting> normalized = normalizeAndValidate(postings);

        return ledger.findByIdempotencyKey(idempotencyKey)
                .map(existing -> {
                    requireSameTransaction(existing, referenceType, referenceId, normalized);
                    return new LedgerPostResult(existing, false);
                })
                .orElseGet(() -> new LedgerPostResult(
                        ledger.save(referenceType, referenceId, idempotencyKey, description, normalized),
                        true
                ));
    }

    public List<LedgerTransaction> history(long accountId, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 200);
        return ledger.findByOwnerAccountId(accountId, safeLimit);
    }

    public void clear() {
        ledger.clear();
    }

    private List<LedgerPosting> normalizeAndValidate(List<LedgerPosting> postings) {
        if (postings == null || postings.size() < 2) {
            throw new IllegalArgumentException("ledger transaction requires at least two entries");
        }

        List<LedgerPosting> normalized = postings.stream().map(posting -> {
            if (posting == null || posting.accountType() == null || posting.direction() == null) {
                throw new IllegalArgumentException("ledger posting fields are required");
            }
            String asset = AssetCatalog.requireSupportedAsset(posting.asset());
            if (posting.amount() == null || posting.amount().signum() <= 0) {
                throw new IllegalArgumentException("ledger amount must be positive");
            }
            if (posting.accountType().isUserAccount() != (posting.ownerAccountId() != null)) {
                throw new IllegalArgumentException("ledger account owner does not match account type");
            }
            return new LedgerPosting(
                    posting.ownerAccountId(),
                    asset,
                    posting.accountType(),
                    posting.direction(),
                    posting.amount().stripTrailingZeros()
            );
        }).toList();

        Map<AssetDirection, BigDecimal> totals = new LinkedHashMap<>();
        for (LedgerPosting posting : normalized) {
            AssetDirection key = new AssetDirection(posting.asset(), posting.direction());
            totals.merge(key, posting.amount(), BigDecimal::add);
        }
        normalized.stream().map(LedgerPosting::asset).distinct().forEach(asset -> {
            BigDecimal debits = totals.getOrDefault(
                    new AssetDirection(asset, EntryDirection.DEBIT), BigDecimal.ZERO
            );
            BigDecimal credits = totals.getOrDefault(
                    new AssetDirection(asset, EntryDirection.CREDIT), BigDecimal.ZERO
            );
            if (debits.compareTo(credits) != 0) {
                throw new IllegalArgumentException("unbalanced ledger transaction for " + asset);
            }
        });
        return normalized;
    }

    private void requireSameTransaction(
            LedgerTransaction existing,
            String referenceType,
            String referenceId,
            List<LedgerPosting> expected
    ) {
        List<LedgerPosting> actual = existing.entries().stream().map(this::toPosting).toList();
        if (!existing.referenceType().equals(referenceType)
                || !existing.referenceId().equals(referenceId)
                || !actual.equals(expected)) {
            throw new IllegalArgumentException("idempotency key was already used with different ledger data");
        }
    }

    private LedgerPosting toPosting(LedgerEntry entry) {
        return new LedgerPosting(
                entry.ownerAccountId(),
                entry.asset(),
                entry.accountType(),
                entry.direction(),
                entry.amount().stripTrailingZeros()
        );
    }

    private void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }

    private record AssetDirection(String asset, EntryDirection direction) {
    }
}
