package com.pasalapeli.bff.service;

import com.pasalapeli.bff.client.TicketClient;
import com.pasalapeli.bff.dto.UsuarioDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class UsuarioContext {

    private final TicketClient ticketClient;

    public UsuarioDTO resolverUsuario(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        String email = authentication.getName();
        String name = authentication.getName();

        if (authentication.getPrincipal() instanceof Jwt jwt) {
            email = jwt.getClaimAsString("preferred_username");
            if (email == null) email = jwt.getClaimAsString("email");
            String givenName = jwt.getClaimAsString("name");
            if (givenName != null) name = givenName;
        }

        // Registra o recupera el usuario real en Ticket Service (find-or-create por correo)
        try {
            return ticketClient.ensureUsuario(email, name);
        } catch (Exception e) {
            log.warn("No se pudo registrar/recuperar el usuario en Ticket Service: {}", e.getMessage());
        }

        // Fallback demo si Ticket Service no responde
        Long fallbackId = roles.contains("ROLE_ADMIN") ? 1L : 2L;
        return UsuarioDTO.builder()
                .id(fallbackId)
                .correo(email)
                .nombre(name)
                .build();
    }
}