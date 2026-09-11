package com.pasalapeli.bff.controller;

import com.pasalapeli.bff.client.TicketClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
@Slf4j
public class TicketBffController {

    private final TicketClient ticketClient;

    @PostMapping("/comprar")
    public ResponseEntity<?> comprarTicket(@RequestBody Object request) {
        log.info("BFF: Recibida solicitud de compra de ticket");
        return ticketClient.comprarTicket(request);
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
