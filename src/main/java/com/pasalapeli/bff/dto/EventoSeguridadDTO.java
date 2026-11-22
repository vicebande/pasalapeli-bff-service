package com.pasalapeli.bff.dto;

/**
 * Evento de seguridad blue-team publicado hacia Amazon Kinesis
 * (stream-security-logs) y luego entregado por Firehose al indice
 * OpenSearch blueteam-logs-*.
 *
 * @param tipo      constante SEGURIDAD
 * @param evento    LOGIN_FALLIDO | ACCESO_DENEGADO | PETICION_SOSPECHOSA | ERROR_SERVIDOR
 * @param timestamp ISO-8601 con offset de America/Santiago
 * @param metodo    metodo HTTP de la peticion
 * @param ruta      ruta + query de la peticion
 * @param status    codigo HTTP de respuesta
 * @param ipOrigen  IP de origen (X-Forwarded-For o remoteAddr)
 * @param userAgent User-Agent de la peticion
 * @param usuario   usuario autenticado si se pudo determinar
 * @param detalle   detalle legible del evento
 */
public record EventoSeguridadDTO(
        String tipo,
        String evento,
        String timestamp,
        String metodo,
        String ruta,
        int status,
        String ipOrigen,
        String userAgent,
        String usuario,
        String detalle) {
}
