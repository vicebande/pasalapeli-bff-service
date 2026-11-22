package com.pasalapeli.bff.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pasalapeli.bff.dto.EventoSeguridadDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kinesis.KinesisClient;
import software.amazon.awssdk.services.kinesis.model.PutRecordRequest;

import java.util.UUID;

/**
 * Publica eventos de seguridad blue-team hacia Amazon Kinesis
 * (stream-security-logs) de forma asincrona y best-effort: una falla de
 * Kinesis solo genera un log local y NUNCA afecta la peticion HTTP.
 *
 * Firehose consume el stream y entrega los documentos al indice OpenSearch
 * blueteam-logs-* (con respaldo de errores en S3).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SecurityLogPublisher {

    private final KinesisClient kinesisClient;
    private final ObjectMapper objectMapper;

    @Value("${app.kinesis.stream-security-logs:stream-security-logs}")
    private String streamName;

    @Async("securityExecutor")
    public void publicar(EventoSeguridadDTO evento) {
        try {
            String payload = objectMapper.writeValueAsString(evento);
            kinesisClient.putRecord(PutRecordRequest.builder()
                    .streamName(streamName)
                    // Particion por IP: los eventos de un mismo origen quedan
                    // juntos en el shard (util para analisis posterior).
                    .partitionKey(evento.ipOrigen() != null && !evento.ipOrigen().isBlank()
                            ? evento.ipOrigen()
                            : UUID.randomUUID().toString())
                    .data(SdkBytes.fromUtf8String(payload))
                    .build());
            log.info("Evento de seguridad publicado en Kinesis: {} {} -> {} (ip={})",
                    evento.evento(), evento.metodo(), evento.ruta(), evento.ipOrigen());
        } catch (Exception ex) {
            log.warn("No se pudo publicar el evento de seguridad en Kinesis ({}): {}",
                    evento.evento(), ex.getMessage());
        }
    }
}
