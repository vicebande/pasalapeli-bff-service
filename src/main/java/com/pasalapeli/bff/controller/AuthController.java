package com.pasalapeli.bff.controller;

import com.pasalapeli.bff.client.TicketClient;
import com.pasalapeli.bff.dto.UserProfileDTO;
import com.pasalapeli.bff.dto.UsuarioDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final TicketClient ticketClient;

    @Value("${azure.activedirectory.client-id:00000000-0000-0000-0000-000000000000}")
    private String clientId;

    @Value("${azure.activedirectory.tenant-id:common}")
    private String tenantId;

    @Value("${azure.activedirectory.enabled:false}")
    private boolean azureEnabled;

    @GetMapping("/login-info")
    public ResponseEntity<Map<String, Object>> getLoginInfo() {
        return ResponseEntity.ok(Map.of(
                "azureEnabled", azureEnabled,
                "clientId", clientId,
                "tenantId", tenantId,
                "authority", "https://login.microsoftonline.com/" + tenantId
        ));
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileDTO> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.ok(UserProfileDTO.builder().authenticated(false).build());
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
        Long userId = null;
        if (email != null) {
            try {
                UsuarioDTO usuario = ticketClient.ensureUsuario(email, name);
                userId = usuario.getId();
            } catch (Exception e) {
                log.warn("No se pudo registrar/recuperar el usuario en Ticket Service: {}", e.getMessage());
            }
        }

        // Fallback demo si Ticket Service no responde
        if (userId == null) {
            userId = roles.contains("ROLE_ADMIN") ? 1L : 2L;
        }

        return ResponseEntity.ok(UserProfileDTO.builder()
                .id(userId)
                .email(email)
                .name(name)
                .roles(roles)
                .authenticated(true)
                .build());
    }
}
