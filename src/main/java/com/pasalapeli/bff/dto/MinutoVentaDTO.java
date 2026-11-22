package com.pasalapeli.bff.dto;

/**
 * Venta agregada en un minuto determinado (chart de lineas del dashboard).
 *
 * @param minuto   etiqueta HH:mm (hora de America/Santiago)
 * @param monto    monto vendido en el minuto (CLP)
 * @param cantidad cantidad de ventas en el minuto
 */
public record MinutoVentaDTO(String minuto, long monto, long cantidad) {
}
