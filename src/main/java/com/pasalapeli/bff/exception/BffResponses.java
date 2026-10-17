package com.pasalapeli.bff.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

/**
 * Cuerpos de error 401/403 del BFF en un unico lugar (antes estaban
 * duplicados inline en cada endpoint).
 */
public final class BffResponses {

    private BffResponses() {
    }

    public static ResponseEntity<Map<String, Object>> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "status", HttpStatus.UNAUTHORIZED.value(),
                "error", "Unauthorized",
                "message", "Usuario no autenticado"
        ));
    }

    public static ResponseEntity<Map<String, Object>> forbidden(String message) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "status", HttpStatus.FORBIDDEN.value(),
                "error", "Forbidden",
                "message", message
        ));
    }
}
