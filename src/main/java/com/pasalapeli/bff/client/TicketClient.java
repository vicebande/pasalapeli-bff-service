package com.pasalapeli.bff.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class TicketClient {

    private final RestTemplate restTemplate;

    @Value("${services.ticket-service.url:http://localhost:8083}")
    private String ticketServiceUrl;

    public ResponseEntity<?> comprarTicket(Object request) {
        String url = ticketServiceUrl + "/api/tickets/comprar";
        try {
            return restTemplate.postForEntity(url, request, Object.class);
        } catch (HttpClientErrorException e) {
            log.warn("Error recibido desde Ticket Service: Status {}", e.getStatusCode());
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        }
    }

    public Object obtenerPorId(Long id) {
        return restTemplate.getForObject(ticketServiceUrl + "/api/tickets/" + id, Object.class);
    }

    public Object obtenerPorCodigo(String codigo) {
        return restTemplate.getForObject(ticketServiceUrl + "/api/tickets/codigo/" + codigo, Object.class);
    }

    public List<?> listarPorUsuario(Long usuarioId) {
        return restTemplate.getForObject(ticketServiceUrl + "/api/tickets/usuario/" + usuarioId, List.class);
    }

    public List<?> listarTodos() {
        return restTemplate.getForObject(ticketServiceUrl + "/api/tickets", List.class);
    }
}
