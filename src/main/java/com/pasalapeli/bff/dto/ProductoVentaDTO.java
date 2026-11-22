package com.pasalapeli.bff.dto;

/**
 * Producto (pelicula) agregado en el ranking de ventas del dia
 * (chart de barras del dashboard).
 *
 * @param producto titulo de la pelicula
 * @param categoria genero de la pelicula
 * @param cantidad cantidad de ventas del producto hoy
 * @param monto    monto vendido del producto hoy (CLP)
 */
public record ProductoVentaDTO(String producto, String categoria, long cantidad, long monto) {
}
