package com.example.exchange.market.websocket;

import com.example.exchange.auth.entity.Account;
import com.example.exchange.auth.repository.AccountRepository;
import com.example.exchange.auth.service.JwtService;
import com.example.exchange.market.dto.MarketDepthResponse;
import com.example.exchange.market.service.BinanceDepthStreamService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MarketWebSocketHandler extends TextWebSocketHandler {
    private static final Logger log = LoggerFactory.getLogger(MarketWebSocketHandler.class);
    private static final int SEND_TIMEOUT_MS = 5_000;
    private static final int SEND_BUFFER_BYTES = 1_048_576;

    private final JwtService jwt;
    private final AccountRepository accounts;
    private final BinanceDepthStreamService depthStreams;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, ClientState> clients = new ConcurrentHashMap<>();

    public MarketWebSocketHandler(
            JwtService jwt,
            AccountRepository accounts,
            BinanceDepthStreamService depthStreams,
            ObjectMapper objectMapper
    ) {
        this.jwt = jwt;
        this.accounts = accounts;
        this.depthStreams = depthStreams;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        WebSocketSession concurrentSession = new ConcurrentWebSocketSessionDecorator(
                session,
                SEND_TIMEOUT_MS,
                SEND_BUFFER_BYTES
        );
        clients.put(session.getId(), new ClientState(concurrentSession));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        ClientState client = clients.get(session.getId());
        if (client == null) {
            close(session, CloseStatus.SERVER_ERROR);
            return;
        }
        try {
            JsonNode payload = objectMapper.readTree(message.getPayload());
            switch (payload.path("type").asText()) {
                case "AUTH" -> authenticate(client, payload.path("token").asText());
                case "SUBSCRIBE" -> subscribe(client, payload.path("symbol").asText());
                default -> sendError(client, "unsupported message type");
            }
        } catch (Exception exception) {
            sendError(client, "invalid websocket message");
        }
    }

    private void authenticate(ClientState client, String token) {
        Optional<JwtService.JwtClaims> verified = jwt.verify(token);
        if (verified.isEmpty()) {
            sendError(client, "invalid or expired token");
            close(client.session, CloseStatus.POLICY_VIOLATION);
            return;
        }
        JwtService.JwtClaims claims = verified.get();
        Optional<Account> account = accounts.findByUsername(claims.username())
                .filter(found -> found.id() == claims.accountId());
        if (account.isEmpty()) {
            sendError(client, "account not found");
            close(client.session, CloseStatus.POLICY_VIOLATION);
            return;
        }
        client.accountId = account.get().id();
        send(client, Map.of("type", "AUTHENTICATED", "accountId", client.accountId));
    }

    private void subscribe(ClientState client, String symbol) {
        if (client.accountId == null) {
            sendError(client, "authenticate before subscribing");
            return;
        }
        closeSubscription(client);
        BinanceDepthStreamService.DepthSubscription subscription = depthStreams.subscribe(
                symbol,
                depth -> sendDepth(client, depth)
        );
        client.subscription = subscription;
        if (!client.session.isOpen()) {
            closeSubscription(client);
            return;
        }
        send(client, Map.of("type", "SUBSCRIBED", "symbol", symbol));
    }

    private void sendDepth(ClientState client, MarketDepthResponse depth) {
        send(client, Map.of("type", "DEPTH", "data", depth));
    }

    private void sendError(ClientState client, String message) {
        send(client, Map.of("type", "ERROR", "message", message));
    }

    private void send(ClientState client, Object payload) {
        if (!client.session.isOpen()) {
            closeSubscription(client);
            return;
        }
        try {
            client.session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
        } catch (IOException exception) {
            log.debug("Cannot send market websocket message: {}", exception.getMessage());
            close(client.session, CloseStatus.SERVER_ERROR);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        ClientState client = clients.remove(session.getId());
        if (client != null) {
            closeSubscription(client);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        close(session, CloseStatus.SERVER_ERROR);
    }

    private void closeSubscription(ClientState client) {
        BinanceDepthStreamService.DepthSubscription subscription = client.subscription;
        client.subscription = null;
        if (subscription != null) {
            subscription.close();
        }
    }

    private void close(WebSocketSession session, CloseStatus status) {
        try {
            if (session.isOpen()) {
                session.close(status);
            }
        } catch (IOException exception) {
            log.debug("Cannot close market websocket: {}", exception.getMessage());
        }
    }

    private static final class ClientState {
        private final WebSocketSession session;
        private volatile Long accountId;
        private volatile BinanceDepthStreamService.DepthSubscription subscription;

        private ClientState(WebSocketSession session) {
            this.session = session;
        }
    }
}
