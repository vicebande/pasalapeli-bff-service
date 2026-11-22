package com.pasalapeli.bff.dto;

/**
 * Evento de venta consumido desde Amazon Kinesis (stream-ventas). El payload
 * lo publica ticket-service con el esquema fijo:
 * { tipo, idVenta, fecha, monto, producto, categoria, ipCliente }.
 */
public record EventoVentaDTO(
        String tipo,
        Long idVenta,
        String fecha,
        Long monto,
        String producto,
        String categoria,
        String ipCliente) {
}
