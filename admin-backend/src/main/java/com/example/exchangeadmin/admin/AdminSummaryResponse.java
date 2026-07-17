package com.example.exchangeadmin.admin;

import java.math.BigDecimal;
import java.util.List;

public record AdminSummaryResponse(
        long accountCount,
        long orderCount,
        long openOrderCount,
        long filledOrderCount,
        long tradeCount,
        BigDecimal stablecoinLiabilityUsdt,
        List<String> supportedMarkets
) {
}
