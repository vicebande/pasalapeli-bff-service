package com.pasalapeli.bff.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Registra el {@link SecurityLogFilter} como el filtro mas externo del
 * contenedor (antes de la cadena de seguridad), de modo que observe el
 * codigo HTTP final de TODAS las peticiones (401/403/5xx/sondeos).
 */
@Configuration
@RequiredArgsConstructor
public class SecurityLogConfig {

    private final SecurityLogPublisher securityLogPublisher;

    @Bean
    public SecurityLogFilter securityLogFilter(ObjectMapper objectMapper) {
        return new SecurityLogFilter(objectMapper, securityLogPublisher);
    }

    @Bean
    public FilterRegistrationBean<SecurityLogFilter> securityLogFilterRegistration(SecurityLogFilter filter) {
        FilterRegistrationBean<SecurityLogFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
