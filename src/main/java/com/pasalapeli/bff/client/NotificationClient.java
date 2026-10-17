package com.pasalapeli.bff.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * Cliente REST hacia el microservicio de notificaciones. El BFF resuelve el
 * usuario autenticado y propaga su id; nunca se confia en el cliente.
 * El manejo de errores POST/DELETE vive en DownstreamClient (sin duplicados).
 */
@Component
@RequiredArgsConstructor
public class NotificationClient {

    private final RestTemplate restTemplate;
    private final DownstreamClient downstream;

    @Value("${services.notification-service.url:http://localhost:8084}")
    private String notificationServiceUrl;

    // ------------------------------ Usuario ------------------------------

    public Object listarPorUsuario(Long usuarioId, int page, int size) {
        return restTemplate.getForObject(
                notificationServiceUrl + "/api/notificaciones/usuario/" + usuarioId
                        + "?page=" + page + "&size=" + size,
                Object.class);
    }

    public Object contarNoLeidas(Long usuarioId) {
        return restTemplate.getForObject(
                notificationServiceUrl + "/api/notificaciones/usuario/" + usuarioId + "/no-leidas/count",
                Object.class);
    }

    public ResponseEntity<?> marcarLeida(Long id, Long usuarioId) {
        return downstream.post(notificationServiceUrl + "/api/notificaciones/" + id + "/leer?usuarioId=" + usuarioId, null, "Notification Service");
    }

    public ResponseEntity<?> marcarTodasLeidas(Long usuarioId) {
        return downstream.post(notificationServiceUrl + "/api/notificaciones/usuario/" + usuarioId + "/leer-todas", null, "Notification Service");
    }

    // ------------------------------ Admin ------------------------------

    public List<?> listarDestinos() {
        return restTemplate.getForObject(notificationServiceUrl + "/api/notificaciones/admin/destinos", List.class);
    }

    public ResponseEntity<?> crearDestino(Object request) {
        return downstream.post(notificationServiceUrl + "/api/notificaciones/admin/destinos", request, "Notification Service");
    }

    public ResponseEntity<?> eliminarDestino(Long id) {
        return downstream.delete(notificationServiceUrl + "/api/notificaciones/admin/destinos/" + id, "Notification Service");
    }

    public List<?> listarReglas() {
        return restTemplate.getForObject(notificationServiceUrl + "/api/notificaciones/admin/reglas", List.class);
    }

    public ResponseEntity<?> crearRegla(Object request) {
        return downstream.post(notificationServiceUrl + "/api/notificaciones/admin/reglas", request, "Notification Service");
    }

    public ResponseEntity<?> eliminarRegla(Long id) {
        return downstream.delete(notificationServiceUrl + "/api/notificaciones/admin/reglas/" + id, "Notification Service");
    }

    public List<?> listarMensajesFallidos() {
        return restTemplate.getForObject(
                notificationServiceUrl + "/api/notificaciones/admin/mensajes-fallidos", List.class);
    }

    public ResponseEntity<?> eliminarMensajeFallido(Long id) {
        return downstream.delete(notificationServiceUrl + "/api/notificaciones/admin/mensajes-fallidos/" + id, "Notification Service");
    }
}
