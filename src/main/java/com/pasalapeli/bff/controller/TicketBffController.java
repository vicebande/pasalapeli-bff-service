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
    public ResponseEntity<?> comprarTicket(@RequestBody Object request) {
        log.info("BFF: Recibida solicitud de compra de ticket");
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

    @GetMapping("/codigo/{codigo}")
    public ResponseEntity<?> obtenerPorCodigo(@PathVariable String codigo) {
        return ResponseEntity.ok(ticketClient.obtenerPorCodigo(codigo));
    }

    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<List<?>> listarPorUsuario(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(ticketClient.listarPorUsuario(usuarioId));
    }
}