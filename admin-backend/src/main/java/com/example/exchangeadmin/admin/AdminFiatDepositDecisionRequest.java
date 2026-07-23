package com.example.exchangeadmin.admin;

public record AdminFiatDepositDecisionRequest(
        String status,
        String failureReason
) {
}
