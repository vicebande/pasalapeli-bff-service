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

import java.util.List;
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
    public ResponseEntity<?> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(ticketClient.obtenerPorId(id));
    }

    @PostMapping("/{id}/devolver")
    public ResponseEntity<?> devolverTicket(Authentication authentication, @PathVariable Long id) {
        UsuarioDTO usuario = usuarioContext.resolverUsuario(authentication);
        if (usuario == null || usuario.getId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "status", HttpStatus.UNAUTHORIZED.value(),
                    "error", "Unauthorized",
                    "message", "Usuario no autenticado"
            ));
        }

        Object ticketObj = ticketClient.obtenerPorId(id);
        if (ticketObj instanceof Map<?, ?> ticketMap) {
            Object propietarioId = ticketMap.get("usuarioId");
            if (propietarioId == null || !String.valueOf(propietarioId).equals(String.valueOf(usuario.getId()))) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                        "status", HttpStatus.FORBIDDEN.value(),
                        "error", "Forbidden",
                        "message", "Solo el dueño del ticket puede realizar la devolución"
                ));
            }
        }

        return ticketClient.devolverTicket(id);
    }

    @GetMapping("/codigo/{codigo}")
    public ResponseEntity<?> obtenerPorCodigo(@PathVariable String codigo) {
        return ResponseEntity.ok(ticketClient.obtenerPorCodigo(codigo));
    }

    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<List<?>> listarPorUsuario(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(ticketClient.listarPorUsuario(usuarioId));
    }
}