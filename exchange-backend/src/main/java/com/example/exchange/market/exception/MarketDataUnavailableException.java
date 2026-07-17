package com.example.exchange.market.exception;

public class MarketDataUnavailableException extends RuntimeException {
    public MarketDataUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
