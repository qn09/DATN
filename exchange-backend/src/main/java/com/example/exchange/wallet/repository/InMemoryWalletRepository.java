package com.example.exchange.wallet.repository;

import com.example.exchange.wallet.entity.WalletBalance;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryWalletRepository implements WalletRepository {
    private final Map<Long, Map<String, WalletBalance>> balances = new HashMap<>();

    @Override
    public Optional<WalletBalance> find(long accountId, String asset) {
        return Optional.ofNullable(balances.getOrDefault(accountId, Map.of()).get(asset));
    }

    @Override
    public List<WalletBalance> findAll(long accountId) {
        return balances.getOrDefault(accountId, Map.of()).values().stream()
                .sorted(Comparator.comparing(WalletBalance::asset))
                .toList();
    }

    @Override
    public void save(long accountId, String asset, BigDecimal available, BigDecimal locked) {
        balances.computeIfAbsent(accountId, ignored -> new HashMap<>())
                .put(asset, new WalletBalance(asset, strip(available), strip(locked)));
    }

    @Override
    public void clear() {
        balances.clear();
    }

    private BigDecimal strip(BigDecimal value) {
        return value.stripTrailingZeros();
    }
}
