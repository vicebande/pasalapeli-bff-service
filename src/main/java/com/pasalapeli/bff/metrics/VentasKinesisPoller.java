package com.pasalapeli.bff.metrics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pasalapeli.bff.dto.EventoVentaDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.kinesis.KinesisClient;
import software.amazon.awssdk.services.kinesis.model.DescribeStreamResponse;
import software.amazon.awssdk.services.kinesis.model.GetRecordsResponse;
import software.amazon.awssdk.services.kinesis.model.Record;
import software.amazon.awssdk.services.kinesis.model.Shard;
import software.amazon.awssdk.services.kinesis.model.ShardIteratorType;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Consumidor de stream-ventas (Amazon Kinesis Data Streams) para el
 * dashboard de metricas en tiempo real.
 *
 * - Polling cada ~1 s con el AWS SDK v2 (KinesisClient sincrono en un hilo
 *   programado; jamas bloquea hilos HTTP).
 * - Al arrancar lee desde TRIM_HORIZON: si la JVM se reinicia, los agregados
 *   en memoria se reconstruyen desde el stream.
 * - Los numeros de secuencia ya procesados se deduplican en memoria para que
 *   un reenvio del iterator (p.ej. iterator expirado) no duplique ventas.
 * - Los registros malformados (p.ej. sondas de prueba) se ignoran.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class VentasKinesisPoller {

    /** Registros por lectura (ciclo de 1 s: holgado para el volumen actual). */
    private static final int LIMITE_REGISTROS = 250;
    /** Ventana de deduplicacion de secuencias (protege reenvios del iterator). */
    private static final int MAX_PROCESADOS = 5000;

    private final KinesisClient kinesisClient;
    private final MetricasAggService metricasAggService;
    private final ObjectMapper objectMapper;

    @Value("${app.kinesis.stream-ventas:stream-ventas}")
    private String streamName;

    /** Shard -> ultimo iterator obtenido. */
    private final Map<String, String> iteradores = new ConcurrentHashMap<>();
    /** Secuencias ya agregadas (shard|secuencia), orden de insercion. */
    private final Set<String> procesados = new LinkedHashSet<>();
    private int fallosSeguidos;

    @Scheduled(fixedDelayString = "${app.metrics.poll-ms:1000}")
    public void poll() {
        try {
            if (iteradores.isEmpty()) {
                inicializar();
            }
            for (Map.Entry<String, String> entry : new ArrayList<>(iteradores.entrySet())) {
                procesarShard(entry.getKey(), entry.getValue());
            }
            if (fallosSeguidos > 0) {
                log.info("Consumidor Kinesis de {} recuperado tras {} fallo(s).", streamName, fallosSeguidos);
            }
            fallosSeguidos = 0;
        } catch (Exception ex) {
            // Se descartan los iterators: el proximo ciclo vuelve a TRIM_HORIZON
            // y la deduplicacion evita contar dos veces.
            iteradores.clear();
            fallosSeguidos++;
            if (fallosSeguidos == 1 || fallosSeguidos % 30 == 0) {
                log.warn("No se pudo consumir {} (intento {}): {}",
                        streamName, fallosSeguidos, ex.getMessage());
            }
        }
    }

    private void inicializar() {
        DescribeStreamResponse desc = kinesisClient.describeStream(b -> b.streamName(streamName));
        for (Shard shard : desc.streamDescription().shards()) {
            String iterator = kinesisClient.getShardIterator(b -> b
                    .streamName(streamName)
                    .shardId(shard.shardId())
                    .shardIteratorType(ShardIteratorType.TRIM_HORIZON)).shardIterator();
            iteradores.put(shard.shardId(), iterator);
        }
        log.info("Consumidor Kinesis listo: {} shard(s) de {}",
                iteradores.size(), streamName);
    }

    private void procesarShard(String shardId, String iterator) {
        GetRecordsResponse respuesta = kinesisClient.getRecords(b -> b
                .shardIterator(iterator)
                .limit(LIMITE_REGISTROS));

        for (Record registro : respuesta.records()) {
            String clave = shardId + "|" + registro.sequenceNumber();
            if (!procesados.add(clave)) {
                continue;
            }
            procesarPayload(registro.data().asUtf8String());
        }
        recortarProcesados();

        String siguiente = respuesta.nextShardIterator();
        if (siguiente != null) {
            iteradores.put(shardId, siguiente);
        } else {
            // Shard cerrado: se re-inicializa en el proximo ciclo.
            iteradores.remove(shardId);
        }
    }

    private void procesarPayload(String json) {
        try {
            EventoVentaDTO evento = objectMapper.readValue(json, EventoVentaDTO.class);
            if (evento.monto() == null) {
                return;
            }
            metricasAggService.registrar(evento);
        } catch (Exception ex) {
            log.debug("Registro no procesable en {}: {}", streamName, ex.getMessage());
        }
    }

    private synchronized void recortarProcesados() {
        while (procesados.size() > MAX_PROCESADOS) {
            var it = procesados.iterator();
            if (it.hasNext()) {
                it.next();
                it.remove();
            } else {
                break;
            }
        }
    }
}
