package com.example.exchange.ledger.dto;

import com.example.exchange.ledger.entity.EntryDirection;
import com.example.exchange.ledger.entity.LedgerAccountType;

import java.math.BigDecimal;

public record LedgerEntryResponse(
        Long ownerAccountId,
        String asset,
        LedgerAccountType accountType,
        EntryDirection direction,
        BigDecimal amount
) {
}
