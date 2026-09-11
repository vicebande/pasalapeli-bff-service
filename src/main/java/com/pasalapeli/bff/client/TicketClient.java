package com.pasalapeli.bff.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pasalapeli.bff.dto.UsuarioDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class TicketClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${services.ticket-service.url:http://localhost:8083}")
    private String ticketServiceUrl;

    public ResponseEntity<?> comprarTicket(Object request) {
        String url = ticketServiceUrl + "/api/tickets/comprar";
        try {
            return restTemplate.postForEntity(url, request, Object.class);
        } catch (HttpClientErrorException e) {
            log.warn("Error recibido desde Ticket Service: Status {}", e.getStatusCode());
            Map<String, Object> errorBody;
            try {
                errorBody = objectMapper.readValue(e.getResponseBodyAsString(),
                        new TypeReference<Map<String, Object>>() {});
            } catch (Exception parseEx) {
                errorBody = new HashMap<>();
                errorBody.put("status", e.getStatusCode().value());
                errorBody.put("error", e.getStatusText());
                errorBody.put("message", e.getResponseBodyAsString());
            }
            return ResponseEntity.status(e.getStatusCode()).body(errorBody);
        }
    }

    public Object obtenerPorId(Long id) {
        return restTemplate.getForObject(ticketServiceUrl + "/api/tickets/" + id, Object.class);
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

    public List<?> listarTodos() {
        return restTemplate.getForObject(ticketServiceUrl + "/api/tickets", List.class);
    }
}
