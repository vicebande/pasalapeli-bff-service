package com.pasalapeli.bff.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pasalapeli.bff.dto.EventoSeguridadDTO;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

/**
 * Filtro blue-team / SIEM del BFF (unica puerta de entrada publica): captura
 * eventos de seguridad y los publica asincronamente en stream-security-logs.
 *
 * Se registra FUERA de la cadena de seguridad (orden maximo) para envolver la
 * peticion completa y observar el codigo HTTP final:
 *   - 401 -> LOGIN_FALLIDO (token ausente/expirado o invalido)
 *   - 403 -> ACCESO_DENEGADO (rol insuficiente, p.ej. /api/admin sin ADMIN)
 *   - >= 500 -> ERROR_SERVIDOR
 *   - patrones de sondeo/inyeccion -> PETICION_SOSPECHOSA (prioridad)
 *
 * El usuario se resuelve sin depender del SecurityContext (que ya fue
 * limpiado al volver la cadena): cabecera X-Dev-User-Email en modo demo o
 * decodificacion (sin validar) del payload del JWT en modo Azure AD.
 * Todo es best-effort: jamas interrumpe la peticion.
 */
@RequiredArgsConstructor
@Slf4j
public class SecurityLogFilter extends OncePerRequestFilter {

    private static final ZoneId ZONA = ZoneId.of("America/Santiago");
    private static final int LIMITE_RUTA = 300;

    /** Rutas que no generan eventos (infraestructura, assets y el propio WS). */
    private static final List<String> RUTAS_IGNORADAS = List.of(
            "/actuator", "/ws", "/error", "/uploads", "/favicon");

    /** Sufijos de assets estaticos (nunca son eventos de seguridad). */
    private static final List<String> SUFIJOS_IGNORADOS = List.of(
            ".js", ".css", ".map", ".png", ".jpg", ".jpeg", ".svg", ".ico", ".woff", ".woff2", ".html");

    /** Patrones de sondeo / inyeccion / traversal clasicos. */
    private static final List<String> PATRONES_SOSPECHOSOS = List.of(
            "../", "..%2f", "%2e%2e", "/etc/passwd", "/etc/shadow", "wp-admin", "wp-login",
            ".env", ".git", "phpmyadmin", "<script", "union select", "sleep(", "or 1=1",
            "cfg.ini", "actuator/env", "actuator/config", "jmx-console", "manager/html");

    private final ObjectMapper objectMapper;
    private final SecurityLogPublisher securityLogPublisher;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        } finally {
            try {
                evaluar(request, response);
            } catch (Exception ex) {
                // El logging de seguridad es best-effort: nunca interrumpe la peticion.
                log.debug("No se pudo evaluar el evento de seguridad: {}", ex.getMessage());
            }
        }
    }

    private void evaluar(HttpServletRequest request, HttpServletResponse response) {
        String ruta = request.getRequestURI();
        if (ignorar(ruta, request.getMethod())) {
            return;
        }

        int status = response.getStatus();
        String rutaCompleta = truncar(ruta + (request.getQueryString() != null
                ? "?" + request.getQueryString() : ""));

        String patronDetectado = detectarPatron(rutaCompleta);
        String evento;
        String detalle;
        if (patronDetectado != null) {
            evento = "PETICION_SOSPECHOSA";
            detalle = "Patron sospechoso detectado: " + patronDetectado;
        } else if (status == HttpServletResponse.SC_UNAUTHORIZED) {
            evento = "LOGIN_FALLIDO";
            detalle = "Token JWT ausente, expirado o invalido (401)";
        } else if (status == HttpServletResponse.SC_FORBIDDEN) {
            evento = "ACCESO_DENEGADO";
            detalle = "Rol insuficiente para el recurso (403)";
        } else if (status >= 500) {
            evento = "ERROR_SERVIDOR";
            detalle = "Error interno durante la peticion (" + status + ")";
        } else {
            // 2xx/3xx y otros 4xx (404, 409...) no son eventos de seguridad.
            return;
        }

        EventoSeguridadDTO dto = new EventoSeguridadDTO(
                "SEGURIDAD",
                evento,
                OffsetDateTime.now(ZONA).toString(),
                request.getMethod(),
                rutaCompleta,
                status,
                ipOrigen(request),
                truncar(request.getHeader("User-Agent")),
                usuario(request),
                detalle);
        securityLogPublisher.publicar(dto);
    }

    private boolean ignorar(String ruta, String metodo) {
        if ("OPTIONS".equalsIgnoreCase(metodo)) {
            return true;
        }
        for (String prefijo : RUTAS_IGNORADAS) {
            if (ruta.startsWith(prefijo)) {
                return true;
            }
        }
        String rutaLower = ruta.toLowerCase(Locale.ROOT);
        for (String sufijo : SUFIJOS_IGNORADOS) {
            if (rutaLower.endsWith(sufijo)) {
                return true;
            }
        }
        return false;
    }

    private String detectarPatron(String rutaLowered) {
        String texto = rutaLowered.toLowerCase(Locale.ROOT);
        for (String patron : PATRONES_SOSPECHOSOS) {
            if (texto.contains(patron)) {
                return patron;
            }
        }
        return null;
    }

    /** IP de origen: primer valor de X-Forwarded-For (nginx) o la conexion. */
    private String ipOrigen(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Usuario para el log: cabecera de identidad del modo demo, o decodificacion
     * SIN VALIDAR del payload del JWT (solo para enriquecer la bitacora; la
     * autenticacion real la hace la cadena de seguridad aguas abajo).
     */
    private String usuario(HttpServletRequest request) {
        String devEmail = request.getHeader("X-Dev-User-Email");
        if (devEmail != null && !devEmail.isBlank()) {
            return devEmail;
        }
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        try {
            String[] partes = authorization.substring(7).split("\\.");
            if (partes.length < 2) {
                return null;
            }
            String payload = partes[1];
            int resto = payload.length() % 4;
            if (resto == 2) {
                payload += "==";
            } else if (resto == 3) {
                payload += "=";
            }
            String json = new String(Base64.getUrlDecoder().decode(payload), StandardCharsets.UTF_8);
            JsonNode nodo = objectMapper.readTree(json);
            for (String campo : new String[]{"preferred_username", "upn", "email", "name", "oid"}) {
                if (nodo.hasNonNull(campo)) {
                    return truncar(nodo.get(campo).asText());
                }
            }
        } catch (Exception ignorar) {
            // Token ilegible: el evento se registra sin usuario.
        }
        return null;
    }

    private String truncar(String valor) {
        if (valor == null) {
            return null;
        }
        String limpio = valor.strip();
        return limpio.length() > LIMITE_RUTA ? limpio.substring(0, LIMITE_RUTA) : limpio;
    }
}
