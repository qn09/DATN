package com.example.exchange.ledger.service;

import com.example.exchange.ledger.entity.LedgerTransaction;

public record LedgerPostResult(LedgerTransaction transaction, boolean created) {
}
