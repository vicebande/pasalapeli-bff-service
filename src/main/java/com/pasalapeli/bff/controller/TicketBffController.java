package com.pasalapeli.bff.controller;

import com.pasalapeli.bff.client.TicketClient;
import com.pasalapeli.bff.dto.UsuarioDTO;
import com.pasalapeli.bff.service.UsuarioContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Map;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
@Slf4j
public class TicketBffController {

    private final TicketClient ticketClient;
    private final UsuarioContext usuarioContext;

    @PostMapping("/comprar")
    public ResponseEntity<?> comprarTicket(Authentication authentication, @RequestBody Map<String, Object> request) {
        log.info("BFF: Recibida solicitud de compra de ticket");
        // El usuario se resuelve desde la autenticación (JWT o cabeceras de demo),
        // nunca se confía en el usuarioId que envía el cliente.
        UsuarioDTO usuario = usuarioContext.resolverUsuario(authentication);
        if (usuario == null || usuario.getId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "status", HttpStatus.UNAUTHORIZED.value(),
                    "error", "Unauthorized",
                    "message", "Usuario no autenticado"
            ));
        }
        request.put("usuarioId", usuario.getId());
        return ticketClient.comprarTicket(request);
    }

    @GetMapping
    public ResponseEntity<?> listarMisTickets(Authentication authentication) {
        UsuarioDTO usuario = usuarioContext.resolverUsuario(authentication);
        if (usuario == null || usuario.getId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "status", HttpStatus.UNAUTHORIZED.value(),
                    "error", "Unauthorized",
                    "message", "Usuario no autenticado"
            ));
        }
        return ResponseEntity.ok(ticketClient.listarPorUsuario(usuario.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(Authentication authentication, @PathVariable Long id) {
        ResponseEntity<?> ownership = verificarOwnership(authentication, id);
        if (ownership != null) {
            return ownership;
        }
        return ResponseEntity.ok(ticketClient.obtenerPorId(id));
    }

    @PostMapping("/{id}/devolver")
    public ResponseEntity<?> devolverTicket(Authentication authentication, @PathVariable Long id) {
        ResponseEntity<?> ownership = verificarOwnership(authentication, id);
        if (ownership != null) {
            return ownership;
        }
        return ticketClient.devolverTicket(id);
    }

    @GetMapping("/codigo/{codigo}")
    public ResponseEntity<?> obtenerPorCodigo(@PathVariable String codigo) {
        return ResponseEntity.ok(ticketClient.obtenerPorCodigo(codigo));
    }

    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<?> listarPorUsuario(Authentication authentication, @PathVariable Long usuarioId) {
        UsuarioDTO usuario = usuarioContext.resolverUsuario(authentication);
        if (usuario == null || usuario.getId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "status", HttpStatus.UNAUTHORIZED.value(),
                    "error", "Unauthorized",
                    "message", "Usuario no autenticado"
            ));
        }
        if (!usuario.getId().equals(usuarioId) && !esAdmin(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "status", HttpStatus.FORBIDDEN.value(),
                    "error", "Forbidden",
                    "message", "Solo puedes consultar tus propios tickets"
            ));
        }
        return ResponseEntity.ok(ticketClient.listarPorUsuario(usuarioId));
    }

    /**
     * Verifica que el ticket pertenezca al usuario autenticado (o que sea ADMIN).
     * Devuelve null si el acceso esta permitido; la respuesta 401/403 correspondiente en caso contrario.
     */
    private ResponseEntity<?> verificarOwnership(Authentication authentication, Long ticketId) {
        UsuarioDTO usuario = usuarioContext.resolverUsuario(authentication);
        if (usuario == null || usuario.getId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "status", HttpStatus.UNAUTHORIZED.value(),
                    "error", "Unauthorized",
                    "message", "Usuario no autenticado"
            ));
        }
        if (esAdmin(authentication)) {
            return null;
        }
        Object ticketObj;
        try {
            ticketObj = ticketClient.obtenerPorId(ticketId);
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).body(Map.of(
                    "status", e.getStatusCode().value(),
                    "error", e.getStatusText(),
                    "message", e.getStatusCode().value() == 404 ? "Ticket no encontrado" : "Acceso denegado"
            ));
        }
        if (ticketObj instanceof Map<?, ?> ticketMap) {
            Object propietarioId = ticketMap.get("usuarioId");
            if (propietarioId == null || !String.valueOf(propietarioId).equals(String.valueOf(usuario.getId()))) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                        "status", HttpStatus.FORBIDDEN.value(),
                        "error", "Forbidden",
                        "message", "Solo el dueno del ticket puede acceder a el"
                ));
            }
        }
        return null;
    }

    private boolean esAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }
}