package com.example.exchangeadmin.common;

import com.example.exchangeadmin.admin.AdminDepositGatewayException;
import com.example.exchangeadmin.market.AdminMarketDataUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class AdminApiExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(AdminMarketDataUnavailableException.class)
    ResponseEntity<Map<String, String>> marketUnavailable(AdminMarketDataUnavailableException exception) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(AdminDepositGatewayException.class)
    ResponseEntity<Map<String, String>> depositGatewayUnavailable(AdminDepositGatewayException exception) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("error", exception.getMessage()));
    }
}
