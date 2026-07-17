package com.example.exchange.ledger.entity;

public enum LedgerAccountType {
    USER_AVAILABLE,
    USER_LOCKED,
    SYSTEM_EXTERNAL,
    SYSTEM_MARKET;

    public boolean isUserAccount() {
        return this == USER_AVAILABLE || this == USER_LOCKED;
    }
}
