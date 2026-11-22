package com.pasalapeli.bff.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket STOMP del dashboard de metricas en tiempo real:
 * - Endpoint publico {@code /ws} (proxyado por nginx hacia este puerto).
 * - Topic {@code /topic/metrics}: el BFF pushcea un MetricasSnapshot cada
 *   ~1,5 s con los agregados de stream-ventas.
 *
 * El endpoint /ws es publico (el handshake del navegador no puede enviar el
 * Bearer de Azure AD); solo expone metricas agregadas, sin datos personales.
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
@Slf4j
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${app.cors.allowed-origins:*}")
    private String corsAllowedOrigins;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Patrones de origen: en produccion restringido a CORS_ALLOWED_ORIGINS.
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(corsAllowedOrigins.split(","));
        log.info("Endpoint STOMP registrado en /ws (topic /topic/metrics)");
    }
}
