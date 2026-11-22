package com.pasalapeli.bff.metrics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Push en tiempo real del dashboard: difunde el snapshot de metricas por el
 * topic STOMP {@code /topic/metrics} cada ~1,5 s. El frontend (Angular +
 * @stomp/stompjs) se suscribe y actualiza los charts sin recargar la pagina.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MetricasBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;
    private final MetricasAggService metricasAggService;

    @Scheduled(fixedDelayString = "${app.metrics.broadcast-ms:1500}")
    public void difundir() {
        try {
            messagingTemplate.convertAndSend("/topic/metrics", metricasAggService.snapshot());
        } catch (Exception ex) {
            // El broker simple es en memoria: un fallo no debe tumbar el scheduler.
            log.debug("No se pudieron difundir las metricas: {}", ex.getMessage());
        }
    }
}
