package com.pasalapeli.bff.controller;

import com.pasalapeli.bff.client.NotificationClient;
import com.pasalapeli.bff.dto.UsuarioDTO;
import com.pasalapeli.bff.service.UsuarioContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * BFF del buzon de notificaciones del usuario autenticado. Resuelve el usuario
 * real y delega en el microservicio de notificaciones.
 * La validacion de sesion vive en UsuarioContext.requerido() (sin duplicados).
 */
@RestController
@RequestMapping("/api/notificaciones")
@RequiredArgsConstructor
@Slf4j
public class NotificationBffController {

    private final NotificationClient notificationClient;
    private final UsuarioContext usuarioContext;

    @GetMapping
    public ResponseEntity<?> listar(Authentication authentication,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "20") int size) {
        UsuarioDTO usuario = usuarioContext.requerido(authentication);
        return ResponseEntity.ok(notificationClient.listarPorUsuario(usuario.getId(), page, size));
    }

    @GetMapping("/no-leidas/count")
    public ResponseEntity<?> contarNoLeidas(Authentication authentication) {
        UsuarioDTO usuario = usuarioContext.requerido(authentication);
        return ResponseEntity.ok(notificationClient.contarNoLeidas(usuario.getId()));
    }

    @PostMapping("/{id}/leer")
    public ResponseEntity<?> marcarLeida(Authentication authentication, @PathVariable Long id) {
        UsuarioDTO usuario = usuarioContext.requerido(authentication);
        return notificationClient.marcarLeida(id, usuario.getId());
    }

    @PostMapping("/leer-todas")
    public ResponseEntity<?> marcarTodasLeidas(Authentication authentication) {
        UsuarioDTO usuario = usuarioContext.requerido(authentication);
        return notificationClient.marcarTodasLeidas(usuario.getId());
    }
}
