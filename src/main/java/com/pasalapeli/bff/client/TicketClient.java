package com.pasalapeli.bff.client;

import com.pasalapeli.bff.dto.UsuarioDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class TicketClient {

    private final RestTemplate restTemplate;
    private final DownstreamClient downstream;

    @Value("${services.ticket-service.url:http://localhost:8083}")
    private String ticketServiceUrl;

    public ResponseEntity<?> comprarTicket(Object request) {
        return downstream.post(ticketServiceUrl + "/api/tickets/comprar", request, "Ticket Service");
    }

    public Object obtenerPorId(Long id) {
        return restTemplate.getForObject(ticketServiceUrl + "/api/tickets/" + id, Object.class);
    }

    public ResponseEntity<?> devolverTicket(Long id) {
        return downstream.post(ticketServiceUrl + "/api/tickets/" + id + "/devolver", null, "Ticket Service");
    }

    public Object obtenerPorCodigo(String codigo) {
        return restTemplate.getForObject(ticketServiceUrl + "/api/tickets/codigo/" + codigo, Object.class);
    }

    public UsuarioDTO ensureUsuario(String correo, String nombre) {
        Map<String, String> body = new HashMap<>();
        body.put("correo", correo == null ? "" : correo);
        body.put("nombre", nombre == null ? "" : nombre);
        return restTemplate.postForObject(ticketServiceUrl + "/api/usuarios/ensure", body, UsuarioDTO.class);
    }

    public List<?> listarPorUsuario(Long usuarioId) {
        return restTemplate.getForObject(ticketServiceUrl + "/api/tickets/usuario/" + usuarioId, List.class);
    }
}
