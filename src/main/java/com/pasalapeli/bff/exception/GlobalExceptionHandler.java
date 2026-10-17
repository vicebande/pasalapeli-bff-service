package com.pasalapeli.bff.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.time.LocalDateTime;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(HttpClientErrorException.class)
    public ResponseEntity<Map<String, Object>> handleHttpClientError(HttpClientErrorException ex) {
        log.warn("BFF recibió error 4xx del downstream: {} -> {}", ex.getStatusCode(), ex.getResponseBodyAsString());
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of(
                "status", ex.getStatusCode().value(),
                "error", ex.getStatusText(),
                "message", ex.getResponseBodyAsString(),
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    @ExceptionHandler(HttpServerErrorException.class)
    public ResponseEntity<Map<String, Object>> handleHttpServerError(HttpServerErrorException ex) {
        log.error("BFF recibió error 5xx del downstream: ", ex);
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of(
                "status", ex.getStatusCode().value(),
                "error", ex.getStatusText(),
                "message", ex.getResponseBodyAsString(),
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        log.error("Error no manejado en BFF: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "error", "Internal Server Error",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()
        ));
    }
}
