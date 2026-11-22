package com.pasalapeli.bff.dto;

import java.util.List;

/**
 * Snapshot agregado de metricas de ventas que se sirve por REST
 * (GET /api/admin/metrics/snapshot) y se pushcea por WebSocket
 * (/topic/metrics) para el dashboard administrativo.
 *
 * @param timestamp   ISO-8601 con offset de America/Santiago
 * @param totalHoy    monto total vendido hoy (CLP, enteros)
 * @param cantidadHoy cantidad de ventas registradas hoy
 * @param porMinuto   ventas por minuto de hoy, orden ascendente
 * @param topProductos productos mas vendidos hoy, orden desc por cantidad
 */
public record MetricasSnapshotDTO(
        String timestamp,
        long totalHoy,
        long cantidadHoy,
        List<MinutoVentaDTO> porMinuto,
        List<ProductoVentaDTO> topProductos) {
}
