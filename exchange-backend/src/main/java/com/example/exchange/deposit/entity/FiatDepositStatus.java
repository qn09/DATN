package com.example.exchange.deposit.entity;

public enum FiatDepositStatus {
    NEW,
    PROCESSING,
    SUCCESS,
    FAILED;

    public boolean isTerminal() {
        return this == SUCCESS || this == FAILED;
    }
}
