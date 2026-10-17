package com.pasalapeli.bff.controller;

import com.pasalapeli.bff.client.NotificationClient;
import com.pasalapeli.bff.dto.UsuarioDTO;
import com.pasalapeli.bff.service.UsuarioContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * BFF del buzon de notificaciones del usuario autenticado. Resuelve el usuario
 * real y delega en el microservicio de notificaciones.
 */
@RestController
@RequestMapping("/api/notificaciones")
@RequiredArgsConstructor
@Slf4j
public class NotificationBffController {

    private final NotificationClient notificationClient;
    private final UsuarioContext usuarioContext;

    @GetMapping
    public ResponseEntity<?> listar(Authentication authentication) {
        UsuarioDTO usuario = usuarioContext.resolverUsuario(authentication);
        if (usuario == null || usuario.getId() == null) {
            return noAutenticado();
        }
        return ResponseEntity.ok(notificationClient.listarPorUsuario(usuario.getId()));
    }

    @GetMapping("/no-leidas/count")
    public ResponseEntity<?> contarNoLeidas(Authentication authentication) {
        UsuarioDTO usuario = usuarioContext.resolverUsuario(authentication);
        if (usuario == null || usuario.getId() == null) {
            return noAutenticado();
        }
        return ResponseEntity.ok(notificationClient.contarNoLeidas(usuario.getId()));
    }

    @PostMapping("/{id}/leer")
    public ResponseEntity<?> marcarLeida(Authentication authentication, @PathVariable Long id) {
        UsuarioDTO usuario = usuarioContext.resolverUsuario(authentication);
        if (usuario == null || usuario.getId() == null) {
            return noAutenticado();
        }
        return notificationClient.marcarLeida(id, usuario.getId());
    }

    @PostMapping("/leer-todas")
    public ResponseEntity<?> marcarTodasLeidas(Authentication authentication) {
        UsuarioDTO usuario = usuarioContext.resolverUsuario(authentication);
        if (usuario == null || usuario.getId() == null) {
            return noAutenticado();
        }
        return notificationClient.marcarTodasLeidas(usuario.getId());
    }

    private ResponseEntity<?> noAutenticado() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "status", HttpStatus.UNAUTHORIZED.value(),
                "error", "Unauthorized",
                "message", "Usuario no autenticado"
        ));
    }
}
