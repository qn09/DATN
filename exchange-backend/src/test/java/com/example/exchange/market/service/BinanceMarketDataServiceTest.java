package com.example.exchange.market.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BinanceMarketDataServiceTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void mapsBinanceKlineTupleToTypedResponse() throws Exception {
        JsonNode row = objectMapper.readTree(
                "[1499040000000,\"10.1\",\"12.2\",\"9.8\",\"11.4\",\"25.5\",1499040059999]"
        );

        var kline = BinanceMarketDataService.toKline(row);

        assertThat(kline.openTime()).isEqualTo(1499040000000L);
        assertThat(kline.close()).isEqualByComparingTo("11.4");
        assertThat(kline.volume()).isEqualByComparingTo("25.5");
    }

    @Test
    void mapsDepthLevelsWithoutUsingFloatingPoint() throws Exception {
        JsonNode rows = objectMapper.readTree("[[\"64850.10\",\"0.025\"],[\"64849.90\",\"1.2\"]]");

        var levels = BinanceMarketDataService.parseDepthLevels(rows);

        assertThat(levels).hasSize(2);
        assertThat(levels.get(0).price()).isEqualByComparingTo(new BigDecimal("64850.10"));
        assertThat(levels.get(0).quantity()).isEqualByComparingTo(new BigDecimal("0.025"));
    }

    @Test
    void rejectsUnknownKlineInterval() {
        assertThatThrownBy(() -> BinanceMarketDataService.requireInterval("7m"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("unsupported kline interval: 7m");
    }
}
