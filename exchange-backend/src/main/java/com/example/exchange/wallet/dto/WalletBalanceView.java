package com.example.exchange.wallet.dto;

import java.math.BigDecimal;

public record WalletBalanceView(
        String asset,
        BigDecimal available,
        BigDecimal locked,
        BigDecimal total,
        BigDecimal priceUsdt,
        BigDecimal availableValueUsdt,
        BigDecimal lockedValueUsdt,
        BigDecimal totalValueUsdt,
        String priceSource
) {
}
