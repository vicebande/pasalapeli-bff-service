package com.pasalapeli.bff.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Filtro de autenticación para modo desarrollo/demo.
 * Si Azure AD está desactivado o se envían cabeceras de desarrollo, inyecta la identidad en el contexto de seguridad.
 */
@Component
@Slf4j
public class DevMockAuthFilter extends OncePerRequestFilter {

    @Value("${azure.activedirectory.enabled:false}")
    private boolean azureEnabled;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Si Azure AD está desactivado o se proveen headers de emulación de usuario
        String devRole = request.getHeader("X-Dev-User-Role");
        String devEmail = request.getHeader("X-Dev-User-Email");

        if (!azureEnabled || devRole != null) {
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
        }

        filterChain.doFilter(request, response);
    }
}
