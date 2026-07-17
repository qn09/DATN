package com.example.exchange.wallet.repository;

import com.example.exchange.wallet.entity.WalletBalance;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface WalletRepository {
    Optional<WalletBalance> find(long accountId, String asset);

    List<WalletBalance> findAll(long accountId);

    void save(long accountId, String asset, BigDecimal available, BigDecimal locked);

    void clear();
}
