package com.example.exchange.market.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MarketPriceServiceTest {
    @Test
    void convertsInternalSymbolToBinanceSymbol() {
        assertThat(MarketPriceService.toBinanceSymbol("btc-usdt")).isEqualTo("BTCUSDT");
        assertThat(MarketPriceService.toBinanceSymbol("eth/usdt")).isEqualTo("ETHUSDT");
    }

    @Test
    void rejectsUnsupportedQuoteAsset() {
        assertThatThrownBy(() -> MarketPriceService.toBinanceSymbol("BTC-VND"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("unsupported market symbol: BTC-VND");
    }
}
