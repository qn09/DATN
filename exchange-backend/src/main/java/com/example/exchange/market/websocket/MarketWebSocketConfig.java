package com.example.exchange.market.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class MarketWebSocketConfig implements WebSocketConfigurer {
    private final MarketWebSocketHandler marketHandler;

    public MarketWebSocketConfig(MarketWebSocketHandler marketHandler) {
        this.marketHandler = marketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(marketHandler, "/ws/market");
    }
}
