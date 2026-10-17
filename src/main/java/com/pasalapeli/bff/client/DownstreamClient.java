package com.pasalapeli.bff.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * POST/DELETE hacia los microservicios con manejo unico de errores 4xx:
 * el cuerpo de error del downstream se reenvia tal cual al cliente.
 * Antes cada cliente duplicaba este mismo bloque.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DownstreamClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public ResponseEntity<?> post(String url, Object body, String servicio) {
        try {
            ResponseEntity<Object> resp = restTemplate.postForEntity(url, body, Object.class);
            return ResponseEntity.status(resp.getStatusCode()).body(resp.getBody());
        } catch (HttpClientErrorException e) {
            log.warn("Error recibido desde {}: Status {}", servicio, e.getStatusCode());
            return ResponseEntity.status(e.getStatusCode()).body(parseError(e));
        }
    }

    public ResponseEntity<?> delete(String url, String servicio) {
        try {
            restTemplate.delete(url);
            return ResponseEntity.noContent().build();
        } catch (HttpClientErrorException e) {
            log.warn("Error recibido desde {} al eliminar: Status {}", servicio, e.getStatusCode());
            return ResponseEntity.status(e.getStatusCode()).body(parseError(e));
        }
    }

    private Map<String, Object> parseError(HttpClientErrorException e) {
        try {
            return objectMapper.readValue(e.getResponseBodyAsString(),
                    new TypeReference<Map<String, Object>>() {});
        } catch (Exception parseEx) {
            Map<String, Object> errorBody = new HashMap<>();
            errorBody.put("status", e.getStatusCode().value());
            errorBody.put("error", e.getStatusText());
            errorBody.put("message", e.getResponseBodyAsString());
            return errorBody;
        }
    }
}
