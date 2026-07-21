package com.example.exchange.market.service;

import com.example.exchange.market.dto.MarketDepthLevel;
import com.example.exchange.market.dto.MarketDepthResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@Service
public class BinanceDepthStreamService {
    private static final Logger log = LoggerFactory.getLogger(BinanceDepthStreamService.class);
    private static final Duration REST_FALLBACK_TTL = Duration.ofSeconds(10);

    private final BinanceMarketDataService marketData;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String streamBaseUrl;
    private final ConcurrentHashMap<String, DepthState> states = new ConcurrentHashMap<>();
    private final ScheduledExecutorService reconnectExecutor = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "binance-depth-reconnect");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicBoolean shuttingDown = new AtomicBoolean(false);

    public BinanceDepthStreamService(
            BinanceMarketDataService marketData,
            ObjectMapper objectMapper,
            @Value("${app.binance.stream-base-url:wss://stream.binance.com:9443/ws}") String streamBaseUrl
    ) {
        this.marketData = marketData;
        this.objectMapper = objectMapper;
        this.streamBaseUrl = streamBaseUrl;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
    }

    public MarketDepthResponse getDepth(String symbol, int limit) {
        String normalizedSymbol = MarketPriceService.normalizeSymbol(symbol);
        int safeLimit = Math.min(Math.max(limit, 1), 20);
        DepthState state = states.computeIfAbsent(normalizedSymbol, DepthState::new);
        connect(state);

        MarketDepthResponse latest = state.latest;
        if (latest == null || (!state.connected && latest.fetchedAt().plus(REST_FALLBACK_TTL).isBefore(Instant.now()))) {
            synchronized (state) {
                latest = state.latest;
                if (latest == null || (!state.connected && latest.fetchedAt().plus(REST_FALLBACK_TTL).isBefore(Instant.now()))) {
                    latest = marketData.getDepthSnapshot(normalizedSymbol, 20);
                    state.latest = latest;
                }
            }
        }
        return limit(latest, safeLimit);
    }

    public DepthSubscription subscribe(String symbol, Consumer<MarketDepthResponse> listener) {
        String normalizedSymbol = MarketPriceService.normalizeSymbol(symbol);
        DepthState state = states.computeIfAbsent(normalizedSymbol, DepthState::new);
        state.listeners.add(listener);
        MarketDepthResponse initial = getDepth(normalizedSymbol, 20);
        listener.accept(initial);
        return () -> state.listeners.remove(listener);
    }

    private void connect(DepthState state) {
        if (shuttingDown.get() || state.connected || !state.connecting.compareAndSet(false, true)) {
            return;
        }
        String stream = MarketPriceService.toBinanceSymbol(state.symbol).toLowerCase() + "@depth20@100ms";
        URI uri = URI.create(streamBaseUrl + "/" + stream);
        httpClient.newWebSocketBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .buildAsync(uri, new DepthListener(state))
                .whenComplete((webSocket, error) -> {
                    state.connecting.set(false);
                    if (error != null) {
                        log.warn("Cannot connect Binance depth stream for {}: {}", state.symbol, error.getMessage());
                        scheduleReconnect(state);
                    }
                });
    }

    private void acceptMessage(DepthState state, String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            long updateId = root.path("lastUpdateId").asLong(root.path("u").asLong());
            if (updateId <= 0 || updateId <= state.lastUpdateId) {
                return;
            }
            List<MarketDepthLevel> bids = BinanceMarketDataService.parseDepthLevels(root.path("bids"));
            List<MarketDepthLevel> asks = BinanceMarketDataService.parseDepthLevels(root.path("asks"));
            if (bids.isEmpty() || asks.isEmpty()) {
                return;
            }
            state.lastUpdateId = updateId;
            MarketDepthResponse depth = new MarketDepthResponse(
                    state.symbol,
                    updateId,
                    bids,
                    asks,
                    "BINANCE_SPOT_WEBSOCKET",
                    Instant.now()
            );
            state.latest = depth;
            state.listeners.forEach(listener -> notifyListener(state, listener, depth));
        } catch (Exception exception) {
            log.warn("Cannot parse Binance depth message for {}: {}", state.symbol, exception.getMessage());
        }
    }

    private void notifyListener(
            DepthState state,
            Consumer<MarketDepthResponse> listener,
            MarketDepthResponse depth
    ) {
        try {
            listener.accept(depth);
        } catch (RuntimeException exception) {
            state.listeners.remove(listener);
            log.debug("Removed failed depth listener for {}: {}", state.symbol, exception.getMessage());
        }
    }

    private void scheduleReconnect(DepthState state) {
        if (!shuttingDown.get() && state.reconnectScheduled.compareAndSet(false, true)) {
            reconnectExecutor.schedule(() -> {
                state.reconnectScheduled.set(false);
                connect(state);
            }, 2, TimeUnit.SECONDS);
        }
    }

    private MarketDepthResponse limit(MarketDepthResponse depth, int limit) {
        return new MarketDepthResponse(
                depth.symbol(),
                depth.lastUpdateId(),
                depth.bids().subList(0, Math.min(limit, depth.bids().size())),
                depth.asks().subList(0, Math.min(limit, depth.asks().size())),
                depth.source(),
                depth.fetchedAt()
        );
    }

    @PreDestroy
    public void close() {
        shuttingDown.set(true);
        states.values().forEach(state -> {
            WebSocket socket = state.socket;
            if (socket != null) {
                socket.sendClose(WebSocket.NORMAL_CLOSURE, "application shutdown");
            }
        });
        reconnectExecutor.shutdownNow();
    }

    private final class DepthListener implements WebSocket.Listener {
        private final DepthState state;
        private final StringBuilder message = new StringBuilder();

        private DepthListener(DepthState state) {
            this.state = state;
        }

        @Override
        public void onOpen(WebSocket webSocket) {
            state.socket = webSocket;
            state.connected = true;
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            message.append(data);
            if (last) {
                String payload = message.toString();
                message.setLength(0);
                acceptMessage(state, payload);
            }
            webSocket.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            state.connected = false;
            state.socket = null;
            scheduleReconnect(state);
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            state.connected = false;
            state.socket = null;
            log.warn("Binance depth stream failed for {}: {}", state.symbol, error.getMessage());
            scheduleReconnect(state);
        }
    }

    private static final class DepthState {
        private final String symbol;
        private final AtomicBoolean connecting = new AtomicBoolean(false);
        private final AtomicBoolean reconnectScheduled = new AtomicBoolean(false);
        private final CopyOnWriteArrayList<Consumer<MarketDepthResponse>> listeners = new CopyOnWriteArrayList<>();
        private volatile boolean connected;
        private volatile long lastUpdateId;
        private volatile MarketDepthResponse latest;
        private volatile WebSocket socket;

        private DepthState(String symbol) {
            this.symbol = symbol;
        }
    }

    @FunctionalInterface
    public interface DepthSubscription extends AutoCloseable {
        @Override
        void close();
    }
}
