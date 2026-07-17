package com.example.exchange.wallet.service;

import com.example.exchange.common.AssetCatalog;
import com.example.exchange.market.dto.MarketPriceResponse;
import com.example.exchange.market.exception.MarketDataUnavailableException;
import com.example.exchange.market.service.MarketPriceService;
import com.example.exchange.wallet.dto.WalletBalanceView;
import com.example.exchange.wallet.entity.WalletBalance;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class WalletValuationService {
    private static final BigDecimal USDT_PRICE = BigDecimal.ONE;
    private static final String INTERNAL_SOURCE = "INTERNAL_STABLECOIN";

    private final WalletService wallets;
    private final MarketPriceService marketPrices;

    public WalletValuationService(WalletService wallets, MarketPriceService marketPrices) {
        this.wallets = wallets;
        this.marketPrices = marketPrices;
    }

    public List<WalletBalanceView> balances(long accountId) {
        return wallets.balances(accountId).stream()
                .map(this::toView)
                .toList();
    }

    private WalletBalanceView toView(WalletBalance balance) {
        BigDecimal total = balance.available().add(balance.locked()).stripTrailingZeros();
        PriceQuote quote = quote(balance.asset());

        return new WalletBalanceView(
                balance.asset(),
                balance.available(),
                balance.locked(),
                total,
                quote.price,
                multiplyOrNull(balance.available(), quote.price),
                multiplyOrNull(balance.locked(), quote.price),
                multiplyOrNull(total, quote.price),
                quote.source
        );
    }

    private PriceQuote quote(String asset) {
        if (AssetCatalog.QUOTE_ASSET.equals(asset)) {
            return new PriceQuote(USDT_PRICE, INTERNAL_SOURCE);
        }

        if (!AssetCatalog.SUPPORTED_ASSETS.contains(asset)) {
            return new PriceQuote(null, null);
        }

        try {
            MarketPriceResponse price = marketPrices.getPrice(asset + "-" + AssetCatalog.QUOTE_ASSET);
            return new PriceQuote(price.price(), price.source());
        } catch (MarketDataUnavailableException exception) {
            return new PriceQuote(null, null);
        }
    }

    private BigDecimal multiplyOrNull(BigDecimal amount, BigDecimal price) {
        if (price == null) {
            return null;
        }
        return amount.multiply(price).stripTrailingZeros();
    }

    private record PriceQuote(BigDecimal price, String source) {
    }
}
