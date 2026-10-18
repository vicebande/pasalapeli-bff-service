package com.pasalapeli.bff.controller;

import com.pasalapeli.bff.client.NotificationClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * BFF de administracion de la mensajeria (destinos, reglas de distribucion y
 * mensajes fallidos). Queda protegido por la regla /api/admin/** -> ROLE_ADMIN.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminNotificacionBffController {

    private final NotificationClient notificationClient;

    // ------------------------------ Destinos ------------------------------

    @GetMapping("/destinos")
    public ResponseEntity<?> listarDestinos() {
        return ResponseEntity.ok(notificationClient.listarDestinos());
    }

    @PostMapping("/destinos")
    public ResponseEntity<?> crearDestino(@RequestBody Map<String, Object> request) {
        return notificationClient.crearDestino(request);
    }

    @DeleteMapping("/destinos/{id}")
    public ResponseEntity<?> eliminarDestino(@PathVariable Long id) {
        return notificationClient.eliminarDestino(id);
    }

    // ------------------------- Reglas de distribucion -------------------------

    @GetMapping("/reglas")
    public ResponseEntity<?> listarReglas() {
        return ResponseEntity.ok(notificationClient.listarReglas());
    }

    @PostMapping("/reglas")
    public ResponseEntity<?> crearRegla(@RequestBody Map<String, Object> request) {
        return notificationClient.crearRegla(request);
    }

    @DeleteMapping("/reglas/{id}")
    public ResponseEntity<?> eliminarRegla(@PathVariable Long id) {
        return notificationClient.eliminarRegla(id);
    }

    // ------------------------------ Anuncio manual ------------------------------

    @PostMapping("/anuncios")
    public ResponseEntity<?> crearAnuncio(@RequestBody Map<String, Object> request) {
        return notificationClient.crearAnuncio(request);
    }

    // --------------------------- Mensajes fallidos ---------------------------

    @GetMapping("/mensajes-fallidos")
    public ResponseEntity<?> listarMensajesFallidos() {
        return ResponseEntity.ok(notificationClient.listarMensajesFallidos());
    }

    @DeleteMapping("/mensajes-fallidos/{id}")
    public ResponseEntity<?> eliminarMensajeFallido(@PathVariable Long id) {
        return notificationClient.eliminarMensajeFallido(id);
    }
}
