package com.example.exchange.wallet.entity;

import java.math.BigDecimal;

public record WalletBalance(String asset, BigDecimal available, BigDecimal locked) {
}
