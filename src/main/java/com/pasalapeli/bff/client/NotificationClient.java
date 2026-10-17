package com.pasalapeli.bff.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cliente REST hacia el microservicio de notificaciones. El BFF resuelve el
 * usuario autenticado y propaga su id; nunca se confia en el cliente.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${services.notification-service.url:http://localhost:8084}")
    private String notificationServiceUrl;

    // ------------------------------ Usuario ------------------------------

    public List<?> listarPorUsuario(Long usuarioId) {
        return restTemplate.getForObject(
                notificationServiceUrl + "/api/notificaciones/usuario/" + usuarioId, List.class);
    }

    public Object contarNoLeidas(Long usuarioId) {
        return restTemplate.getForObject(
                notificationServiceUrl + "/api/notificaciones/usuario/" + usuarioId + "/no-leidas/count",
                Object.class);
    }

    public ResponseEntity<?> marcarLeida(Long id, Long usuarioId) {
        return post(notificationServiceUrl + "/api/notificaciones/" + id + "/leer?usuarioId=" + usuarioId, null);
    }

    public ResponseEntity<?> marcarTodasLeidas(Long usuarioId) {
        return post(notificationServiceUrl + "/api/notificaciones/usuario/" + usuarioId + "/leer-todas", null);
    }

    // ------------------------------ Admin ------------------------------

    public List<?> listarDestinos() {
        return restTemplate.getForObject(notificationServiceUrl + "/api/notificaciones/admin/destinos", List.class);
    }

    public ResponseEntity<?> crearDestino(Object request) {
        return post(notificationServiceUrl + "/api/notificaciones/admin/destinos", request);
    }

    public ResponseEntity<?> eliminarDestino(Long id) {
        return delete(notificationServiceUrl + "/api/notificaciones/admin/destinos/" + id);
    }

    public List<?> listarReglas() {
        return restTemplate.getForObject(notificationServiceUrl + "/api/notificaciones/admin/reglas", List.class);
    }

    public ResponseEntity<?> crearRegla(Object request) {
        return post(notificationServiceUrl + "/api/notificaciones/admin/reglas", request);
    }

    public ResponseEntity<?> eliminarRegla(Long id) {
        return delete(notificationServiceUrl + "/api/notificaciones/admin/reglas/" + id);
    }

    public List<?> listarMensajesFallidos() {
        return restTemplate.getForObject(
                notificationServiceUrl + "/api/notificaciones/admin/mensajes-fallidos", List.class);
    }

    public ResponseEntity<?> eliminarMensajeFallido(Long id) {
        return delete(notificationServiceUrl + "/api/notificaciones/admin/mensajes-fallidos/" + id);
    }

    // ------------------------------ Helpers ------------------------------

    private ResponseEntity<?> post(String url, Object body) {
        try {
            ResponseEntity<Object> resp = restTemplate.postForEntity(url, body, Object.class);
            return ResponseEntity.status(resp.getStatusCode()).body(resp.getBody());
        } catch (HttpClientErrorException e) {
            log.warn("Error recibido desde Notification Service: Status {}", e.getStatusCode());
            return ResponseEntity.status(e.getStatusCode()).body(parseError(e));
        }
    }

    private ResponseEntity<?> delete(String url) {
        try {
            restTemplate.delete(url);
            return ResponseEntity.noContent().build();
        } catch (HttpClientErrorException e) {
            log.warn("Error recibido desde Notification Service al eliminar: Status {}", e.getStatusCode());
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
