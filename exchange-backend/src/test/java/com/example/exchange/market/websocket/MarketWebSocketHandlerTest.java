package com.example.exchange.market.websocket;

import com.example.exchange.auth.entity.Account;
import com.example.exchange.auth.entity.Role;
import com.example.exchange.auth.repository.AccountRepository;
import com.example.exchange.auth.service.JwtService;
import com.example.exchange.market.service.BinanceDepthStreamService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MarketWebSocketHandlerTest {
    private final JwtService jwt = mock(JwtService.class);
    private final AccountRepository accounts = mock(AccountRepository.class);
    private final BinanceDepthStreamService depthStreams = mock(BinanceDepthStreamService.class);
    private final WebSocketSession session = mock(WebSocketSession.class);
    private final MarketWebSocketHandler handler = new MarketWebSocketHandler(
            jwt,
            accounts,
            depthStreams,
            new ObjectMapper()
    );

    @BeforeEach
    void connect() {
        when(session.getId()).thenReturn("session-1");
        when(session.isOpen()).thenReturn(true);
        handler.afterConnectionEstablished(session);
    }

    @Test
    void requiresAuthenticationBeforeSubscribe() throws Exception {
        handler.handleMessage(session, new TextMessage("{\"type\":\"SUBSCRIBE\",\"symbol\":\"BTC-USDT\"}"));

        ArgumentCaptor<WebSocketMessage<?>> message = ArgumentCaptor.forClass(WebSocketMessage.class);
        verify(session).sendMessage(message.capture());
        assertThat(message.getValue().getPayload().toString())
                .contains("ERROR")
                .contains("authenticate before subscribing");
    }

    @Test
    void authenticatesJwtBeforeCreatingDepthSubscription() throws Exception {
        Account account = new Account(6L, "buyer", "hash", Role.USER);
        when(jwt.verify("valid-token"))
                .thenReturn(Optional.of(new JwtService.JwtClaims("buyer", 6L, Role.USER)));
        when(accounts.findByUsername("buyer")).thenReturn(Optional.of(account));
        when(depthStreams.subscribe(eq("BTC-USDT"), any()))
                .thenReturn(() -> { });

        handler.handleMessage(session, new TextMessage("{\"type\":\"AUTH\",\"token\":\"valid-token\"}"));
        handler.handleMessage(session, new TextMessage("{\"type\":\"SUBSCRIBE\",\"symbol\":\"BTC-USDT\"}"));

        verify(depthStreams).subscribe(eq("BTC-USDT"), any());
        ArgumentCaptor<WebSocketMessage<?>> messages = ArgumentCaptor.forClass(WebSocketMessage.class);
        verify(session, atLeast(2)).sendMessage(messages.capture());
        List<String> payloads = messages.getAllValues().stream()
                .map(message -> message.getPayload().toString())
                .toList();
        assertThat(payloads).anyMatch(payload -> payload.contains("AUTHENTICATED"));
        assertThat(payloads).anyMatch(payload -> payload.contains("SUBSCRIBED"));
    }
}
