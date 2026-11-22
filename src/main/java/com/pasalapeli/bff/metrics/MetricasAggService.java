package com.pasalapeli.bff.metrics;

import com.pasalapeli.bff.dto.EventoVentaDTO;
import com.pasalapeli.bff.dto.MetricasSnapshotDTO;
import com.pasalapeli.bff.dto.MinutoVentaDTO;
import com.pasalapeli.bff.dto.ProductoVentaDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Agregacion en memoria de las ventas consumidas de stream-ventas:
 * total del dia, ventas por minuto (para el chart de lineas) y ranking de
 * productos (chart de barras). Todo el estado vive en la JVM del BFF y se
 * reconstruye desde el stream al reiniciar (el poller arranca en
 * TRIM_HORIZON), por lo que no requiere base de datos ni Redis.
 *
 * Los metodos son synchronized: los escribe el hilo del poller de Kinesis y
 * leen los hilos HTTP/WebSocket (contencion minima por el tamano de los datos).
 */
@Service
@Slf4j
public class MetricasAggService {

    /** Zona horaria de negocio (mismas reglas que los servicios de dominio). */
    private static final ZoneId ZONA = ZoneId.of("America/Santiago");
    /** Minutos visibles en el chart de lineas (ultimos N del dia). */
    private static final int MAX_MINUTOS_GRAFICO = 30;
    /** Tamanio del ranking de productos. */
    private static final int TOP_PRODUCTOS = 5;

    /** Clave del dia agregado actual (yyyy-MM-dd); al cambiar se limpia todo. */
    private String diaClave;
    private long totalHoy;
    private long cantidadHoy;
    /** Ventas del dia: clave "HH:mm" -> acumulado del minuto. */
    private final Map<String, Acumulador> minutosHoy = new LinkedHashMap<>();
    /** Ranking del dia: clave "producto|categoria" -> acumulado del producto. */
    private final Map<String, ProductoAcumulador> productosHoy = new LinkedHashMap<>();

    /** Registra una venta consumida del stream (ignora registros malformados). */
    public synchronized void registrar(EventoVentaDTO evento) {
        if (evento == null || evento.monto() == null) {
            return;
        }
        OffsetDateTime fecha = parseFecha(evento.fecha());
        LocalDate hoy = LocalDate.now(ZONA);
        LocalDate diaVenta = fecha != null ? fecha.toLocalDate() : hoy;
        if (!diaVenta.equals(hoy)) {
            // Fuera del dia en curso: el dashboard solo muestra "hoy".
            return;
        }
        resetSiCambioDia(hoy);

        String minuto = fecha != null
                ? String.format("%02d:%02d", fecha.getHour(), fecha.getMinute())
                : String.format("%02d:%02d",
                        OffsetDateTime.now(ZONA).getHour(), OffsetDateTime.now(ZONA).getMinute());

        minutosHoy.computeIfAbsent(minuto, k -> new Acumulador()).sumar(evento.monto());

        String claveProducto = clave(evento.producto(), evento.categoria());
        ProductoAcumulador prod = productosHoy.computeIfAbsent(claveProducto,
                k -> new ProductoAcumulador(evento.producto(), evento.categoria()));
        prod.sumar(evento.monto());

        totalHoy += evento.monto();
        cantidadHoy++;
        log.debug("Venta agregada: id={}, monto={}, minuto={}, totalHoy={}",
                evento.idVenta(), evento.monto(), minuto, totalHoy);
    }

    /** Snapshot actual para REST (/snapshot) y para el push por WebSocket. */
    public synchronized MetricasSnapshotDTO snapshot() {
        resetSiCambioDia(LocalDate.now(ZONA));

        List<MinutoVentaDTO> porMinuto = minutosHoy.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .skip(Math.max(0, minutosHoy.size() - MAX_MINUTOS_GRAFICO))
                .map(e -> new MinutoVentaDTO(e.getKey(), e.getValue().monto, e.getValue().cantidad))
                .toList();

        List<ProductoVentaDTO> topProductos = productosHoy.values().stream()
                .sorted(Comparator.comparingLong((ProductoAcumulador p) -> p.cantidad).reversed()
                        .thenComparing(Comparator.comparingLong((ProductoAcumulador p) -> p.monto).reversed()))
                .limit(TOP_PRODUCTOS)
                .map(p -> new ProductoVentaDTO(p.producto, p.categoria, p.cantidad, p.monto))
                .toList();

        return new MetricasSnapshotDTO(
                OffsetDateTime.now(ZONA).toString(),
                totalHoy,
                cantidadHoy,
                porMinuto,
                topProductos);
    }

    /** Limpia los agregados cuando cambia el dia en America/Santiago. */
    private void resetSiCambioDia(LocalDate hoy) {
        String clave = hoy.toString();
        if (diaClave == null || !diaClave.equals(clave)) {
            diaClave = clave;
            totalHoy = 0;
            cantidadHoy = 0;
            minutosHoy.clear();
            productosHoy.clear();
            if (diaClave != null) {
                log.info("Metricas reiniciadas para el dia {}", clave);
            }
        }
    }

    /** Fecha del evento: ISO con offset, luego wall-time, luego ahora. */
    private OffsetDateTime parseFecha(String fecha) {
        if (fecha == null || fecha.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(fecha);
        } catch (DateTimeParseException ignorar) {
            try {
                return java.time.LocalDateTime.parse(fecha).atZone(ZONA).toOffsetDateTime();
            } catch (DateTimeParseException ignorar2) {
                log.debug("Fecha de venta no reconocida: {}", fecha);
                return null;
            }
        }
    }

    private String clave(String producto, String categoria) {
        return (producto != null ? producto : "Sin producto") + "|"
                + (categoria != null ? categoria : "Sin categoria");
    }

    /** Acumulador generico de monto + cantidad. */
    private static final class Acumulador {
        private long monto;
        private long cantidad;

        void sumar(long valor) {
            monto += valor;
            cantidad++;
        }
    }

    /** Acumulador etiquetado para el ranking de productos. */
    private static final class ProductoAcumulador {
        private final String producto;
        private final String categoria;
        private long monto;
        private long cantidad;

        ProductoAcumulador(String producto, String categoria) {
            this.producto = producto != null ? producto : "Sin producto";
            this.categoria = categoria != null ? categoria : "Sin categoria";
        }

        void sumar(long valor) {
            monto += valor;
            cantidad++;
        }
    }
}
