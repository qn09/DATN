package com.example.exchange.wallet.service;

import com.example.exchange.common.AssetCatalog;
import com.example.exchange.ledger.entity.EntryDirection;
import com.example.exchange.ledger.entity.LedgerAccountType;
import com.example.exchange.ledger.entity.LedgerPosting;
import com.example.exchange.ledger.service.LedgerPostResult;
import com.example.exchange.ledger.service.LedgerService;
import com.example.exchange.trading.entity.MarketSymbol;
import com.example.exchange.trading.entity.Order;
import com.example.exchange.trading.entity.Side;
import com.example.exchange.wallet.entity.WalletBalance;
import com.example.exchange.wallet.repository.InMemoryWalletRepository;
import com.example.exchange.wallet.repository.WalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class WalletService {
    private final WalletRepository wallets;
    private final LedgerService ledger;

    public WalletService() {
        this(new InMemoryWalletRepository(), new LedgerService());
    }

    public WalletService(WalletRepository wallets) {
        this(wallets, new LedgerService());
    }

    @Autowired
    public WalletService(WalletRepository wallets, LedgerService ledger) {
        this.wallets = wallets;
        this.ledger = ledger;
    }

    @Transactional
    public synchronized void deposit(long accountId, String asset, BigDecimal amount) {
        deposit(accountId, asset, amount, UUID.randomUUID().toString());
    }

    @Transactional
    public synchronized void deposit(
            long accountId,
            String asset,
            BigDecimal amount,
            String idempotencyKey
    ) {
        requirePositive(amount, "amount");
        String normalizedAsset = AssetCatalog.requireSupportedAsset(asset);
        String requestKey = requireIdempotencyKey(idempotencyKey);
        postAndProject(
                "DEPOSIT",
                requestKey,
                "deposit:" + accountId + ":" + requestKey,
                "Deposit " + amount.stripTrailingZeros().toPlainString() + " " + normalizedAsset,
                List.of(
                        LedgerPosting.system(
                                normalizedAsset,
                                LedgerAccountType.SYSTEM_EXTERNAL,
                                EntryDirection.DEBIT,
                                amount
                        ),
                        LedgerPosting.user(
                                accountId,
                                normalizedAsset,
                                LedgerAccountType.USER_AVAILABLE,
                                EntryDirection.CREDIT,
                                amount
                        )
                )
        );
    }

    public synchronized List<WalletBalance> balances(long accountId) {
        return wallets.findAll(accountId).stream()
                .sorted(Comparator.comparing(WalletBalance::asset))
                .toList();
    }

    @Transactional
    public synchronized void reserveForOrder(
            long accountId,
            MarketSymbol market,
            Side side,
            BigDecimal price,
            BigDecimal quantity
    ) {
        requirePositive(price, "price");
        requirePositive(quantity, "quantity");

        String asset = side == Side.BUY ? market.quoteAsset() : market.baseAsset();
        BigDecimal amount = side == Side.BUY ? price.multiply(quantity) : quantity;
        String referenceId = UUID.randomUUID().toString();
        postAndProject(
                "ORDER_RESERVE",
                referenceId,
                "order-reserve:" + referenceId,
                "Reserve " + amount.stripTrailingZeros().toPlainString() + " " + asset + " for order",
                List.of(
                        LedgerPosting.user(
                                accountId,
                                asset,
                                LedgerAccountType.USER_AVAILABLE,
                                EntryDirection.DEBIT,
                                amount
                        ),
                        LedgerPosting.user(
                                accountId,
                                asset,
                                LedgerAccountType.USER_LOCKED,
                                EntryDirection.CREDIT,
                                amount
                        )
                )
        );
    }

    @Transactional
    public synchronized void settleTrade(
            MarketSymbol market,
            Order buy,
            Order sell,
            BigDecimal price,
            BigDecimal quantity
    ) {
        BigDecimal reservedQuoteAmount = buy.getPrice().multiply(quantity);
        BigDecimal executedQuoteAmount = price.multiply(quantity);
        BigDecimal priceImprovement = reservedQuoteAmount.subtract(executedQuoteAmount);

        List<LedgerPosting> postings = new ArrayList<>(List.of(
                LedgerPosting.user(
                        buy.getAccountId(), market.quoteAsset(), LedgerAccountType.USER_LOCKED,
                        EntryDirection.DEBIT, reservedQuoteAmount
                ),
                LedgerPosting.user(
                        sell.getAccountId(), market.quoteAsset(), LedgerAccountType.USER_AVAILABLE,
                        EntryDirection.CREDIT, executedQuoteAmount
                ),
                LedgerPosting.user(
                        sell.getAccountId(), market.baseAsset(), LedgerAccountType.USER_LOCKED,
                        EntryDirection.DEBIT, quantity
                ),
                LedgerPosting.user(
                        buy.getAccountId(), market.baseAsset(), LedgerAccountType.USER_AVAILABLE,
                        EntryDirection.CREDIT, quantity
                )
        ));
        if (priceImprovement.signum() > 0) {
            postings.add(LedgerPosting.user(
                    buy.getAccountId(), market.quoteAsset(), LedgerAccountType.USER_AVAILABLE,
                    EntryDirection.CREDIT, priceImprovement
            ));
        }

        String referenceId = buy.getId() + ":" + sell.getId() + ":" + UUID.randomUUID();
        postAndProject(
                "INTERNAL_TRADE",
                referenceId,
                "internal-trade:" + referenceId,
                "Settle internal trade " + symbol(market),
                postings
        );
    }

    @Transactional
    public synchronized void settleMarketBuy(
            MarketSymbol market,
            Order buy,
            BigDecimal executionPrice,
            BigDecimal quantity
    ) {
        BigDecimal reservedQuoteAmount = buy.getPrice().multiply(quantity);
        BigDecimal executedQuoteAmount = executionPrice.multiply(quantity);
        BigDecimal refund = reservedQuoteAmount.subtract(executedQuoteAmount);

        List<LedgerPosting> postings = new ArrayList<>(List.of(
                LedgerPosting.user(
                        buy.getAccountId(), market.quoteAsset(), LedgerAccountType.USER_LOCKED,
                        EntryDirection.DEBIT, reservedQuoteAmount
                ),
                LedgerPosting.system(
                        market.quoteAsset(), LedgerAccountType.SYSTEM_MARKET,
                        EntryDirection.CREDIT, executedQuoteAmount
                ),
                LedgerPosting.system(
                        market.baseAsset(), LedgerAccountType.SYSTEM_MARKET,
                        EntryDirection.DEBIT, quantity
                ),
                LedgerPosting.user(
                        buy.getAccountId(), market.baseAsset(), LedgerAccountType.USER_AVAILABLE,
                        EntryDirection.CREDIT, quantity
                )
        ));
        if (refund.signum() > 0) {
            postings.add(LedgerPosting.user(
                    buy.getAccountId(), market.quoteAsset(), LedgerAccountType.USER_AVAILABLE,
                    EntryDirection.CREDIT, refund
            ));
        }

        String referenceId = String.valueOf(buy.getId());
        postAndProject(
                "MARKET_BUY",
                referenceId,
                "market-buy:" + buy.getId(),
                "Settle Binance market buy " + symbol(market),
                postings
        );
    }

    @Transactional
    public synchronized void settleMarketSell(
            MarketSymbol market,
            Order sell,
            BigDecimal executionPrice,
            BigDecimal quantity
    ) {
        BigDecimal quoteAmount = executionPrice.multiply(quantity);
        postAndProject(
                "MARKET_SELL",
                String.valueOf(sell.getId()),
                "market-sell:" + sell.getId(),
                "Settle Binance market sell " + symbol(market),
                List.of(
                        LedgerPosting.user(
                                sell.getAccountId(), market.baseAsset(), LedgerAccountType.USER_LOCKED,
                                EntryDirection.DEBIT, quantity
                        ),
                        LedgerPosting.system(
                                market.baseAsset(), LedgerAccountType.SYSTEM_MARKET,
                                EntryDirection.CREDIT, quantity
                        ),
                        LedgerPosting.system(
                                market.quoteAsset(), LedgerAccountType.SYSTEM_MARKET,
                                EntryDirection.DEBIT, quoteAmount
                        ),
                        LedgerPosting.user(
                                sell.getAccountId(), market.quoteAsset(), LedgerAccountType.USER_AVAILABLE,
                                EntryDirection.CREDIT, quoteAmount
                        )
                )
        );
    }

    public synchronized void clear() {
        ledger.clear();
        wallets.clear();
    }

    private boolean postAndProject(
            String referenceType,
            String referenceId,
            String idempotencyKey,
            String description,
            List<LedgerPosting> postings
    ) {
        Map<BalanceKey, Balance> changedBalances = project(postings);
        LedgerPostResult result = ledger.post(
                referenceType,
                referenceId,
                idempotencyKey,
                description,
                postings
        );
        if (!result.created()) {
            return false;
        }
        changedBalances.forEach((key, balance) -> wallets.save(
                key.accountId(),
                key.asset(),
                balance.available,
                balance.locked
        ));
        return true;
    }

    private Map<BalanceKey, Balance> project(List<LedgerPosting> postings) {
        Map<BalanceKey, Balance> changedBalances = new LinkedHashMap<>();
        for (LedgerPosting posting : postings) {
            if (!posting.accountType().isUserAccount()) {
                continue;
            }
            BalanceKey key = new BalanceKey(posting.ownerAccountId(), posting.asset());
            Balance balance = changedBalances.computeIfAbsent(
                    key,
                    ignored -> balance(key.accountId(), key.asset())
            );
            BigDecimal delta = posting.direction() == EntryDirection.CREDIT
                    ? posting.amount()
                    : posting.amount().negate();
            if (posting.accountType() == LedgerAccountType.USER_AVAILABLE) {
                balance.available = balance.available.add(delta);
            } else {
                balance.locked = balance.locked.add(delta);
            }
        }

        changedBalances.forEach((key, balance) -> {
            if (balance.available.signum() < 0) {
                throw new IllegalArgumentException("insufficient available " + key.asset());
            }
            if (balance.locked.signum() < 0) {
                throw new IllegalArgumentException("insufficient locked " + key.asset());
            }
        });
        return changedBalances;
    }

    private Balance balance(long accountId, String asset) {
        WalletBalance walletBalance = wallets.find(accountId, asset)
                .orElseGet(() -> new WalletBalance(asset, BigDecimal.ZERO, BigDecimal.ZERO));
        return new Balance(walletBalance.available(), walletBalance.locked());
    }

    private String requireIdempotencyKey(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Idempotency-Key is required");
        }
        String normalized = value.trim();
        if (normalized.length() > 128) {
            throw new IllegalArgumentException("Idempotency-Key must not exceed 128 characters");
        }
        return normalized;
    }

    private String symbol(MarketSymbol market) {
        return market.baseAsset() + "-" + market.quoteAsset();
    }

    private void requirePositive(BigDecimal value, String field) {
        if (value == null || value.signum() <= 0) {
            throw new IllegalArgumentException(field + " must be positive");
        }
    }

    private record BalanceKey(long accountId, String asset) {
    }

    private static class Balance {
        private BigDecimal available;
        private BigDecimal locked;

        private Balance(BigDecimal available, BigDecimal locked) {
            this.available = available;
            this.locked = locked;
        }
    }
}
