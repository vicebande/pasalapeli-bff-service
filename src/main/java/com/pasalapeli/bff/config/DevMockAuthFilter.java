package com.pasalapeli.bff.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Filtro de autenticacion para modo desarrollo/demo.
 * SOLO actua cuando Azure AD esta desactivado ({@code azure.activedirectory.enabled=false}).
 * Con Azure activo el filtro es inerte: las cabeceras X-Dev-User-* NUNCA se consideran,
 * de modo que no es posible suplantar identidades (spoofing) en produccion.
 *
 * No es un {@code @Component} a proposito: asi Spring Boot no lo registra como filtro
 * servlet fuera de la cadena de seguridad; solo se inyecta en SecurityConfig.
 */
@Slf4j
public class DevMockAuthFilter extends OncePerRequestFilter {

    private final boolean azureEnabled;

    public DevMockAuthFilter(boolean azureEnabled) {
        this.azureEnabled = azureEnabled;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (!azureEnabled) {
            String devRole = request.getHeader("X-Dev-User-Role");
            String devEmail = request.getHeader("X-Dev-User-Email");

            String role = (devRole != null && !devRole.isBlank()) ? devRole.toUpperCase() : "CLIENTE";
            String email = (devEmail != null && !devEmail.isBlank()) ? devEmail : "usuario.demo@pasalapeli.cl";

            List<SimpleGrantedAuthority> authorities = new ArrayList<>();
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
            if ("ADMIN".equals(role)) {
                authorities.add(new SimpleGrantedAuthority("ROLE_CLIENTE"));
            }

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(email, null, authorities);

            SecurityContextHolder.getContext().setAuthentication(auth);
        } else if (request.getHeader("X-Dev-User-Role") != null) {
            log.warn("Cabeceras X-Dev-User-* ignoradas: Azure AD esta activo");
        }

        filterChain.doFilter(request, response);
    }
}
