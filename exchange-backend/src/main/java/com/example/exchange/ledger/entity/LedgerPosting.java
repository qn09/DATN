package com.example.exchange.ledger.entity;

import java.math.BigDecimal;

public record LedgerPosting(
        Long ownerAccountId,
        String asset,
        LedgerAccountType accountType,
        EntryDirection direction,
        BigDecimal amount
) {
    public static LedgerPosting user(
            long accountId,
            String asset,
            LedgerAccountType accountType,
            EntryDirection direction,
            BigDecimal amount
    ) {
        return new LedgerPosting(accountId, asset, accountType, direction, amount);
    }

    public static LedgerPosting system(
            String asset,
            LedgerAccountType accountType,
            EntryDirection direction,
            BigDecimal amount
    ) {
        return new LedgerPosting(null, asset, accountType, direction, amount);
    }
}
