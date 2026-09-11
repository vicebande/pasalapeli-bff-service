package com.pasalapeli.bff.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    private final JwtAuthConverter jwtAuthConverter;
    private final DevMockAuthFilter devMockAuthFilter;

    @Value("${azure.activedirectory.enabled:false}")
    private boolean azureEnabled;

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:https://login.microsoftonline.com/common/discovery/v2.0/keys}")
    private String jwkSetUri;

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:https://login.microsoftonline.com/common/v2.0}")
    private String issuerUri;

    @Value("${azure.activedirectory.client-id:00000000-0000-0000-0000-000000000000}")
    private String clientId;

    @Value("${azure.activedirectory.app-id-uri:api://00000000-0000-0000-0000-000000000000}")
    private String appIdUri;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Rutas públicas: Cartelera, detalles de película, consulta de funciones
                        .requestMatchers(HttpMethod.GET, "/api/cartelera/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/login-info").permitAll()
                        .requestMatchers("/error").permitAll()
                        // Rutas exclusivas para administradores
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // Rutas protegidas para clientes y administradores
                        .requestMatchers("/api/tickets/**").authenticated()
                        .requestMatchers("/api/auth/me").authenticated()
                        .anyRequest().authenticated()
                );

        if (azureEnabled) {
            // Validador completo de Azure AD según rúbrica (firma, expiración, issuer, audience)
            http.oauth2ResourceServer(oauth2 -> oauth2
                    .jwt(jwt -> jwt
                            .decoder(jwtDecoder())
                            .jwtAuthenticationConverter(jwtAuthConverter)
                    )
                    .authenticationEntryPoint((request, response, authException) -> {
                        log.warn("Acceso denegado (401 Unauthorized): {}", authException.getMessage());
                        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                        response.setStatus(HttpStatus.UNAUTHORIZED.value());
                        Map<String, Object> body = Map.of(
                                "status", HttpStatus.UNAUTHORIZED.value(),
                                "error", "Unauthorized",
                                "message", "Token JWT ausente, expirado o inválido: " + authException.getMessage(),
                                "timestamp", LocalDateTime.now().toString()
                        );
                        new ObjectMapper().writeValue(response.getOutputStream(), body);
                    })
                    .accessDeniedHandler((request, response, accessDeniedException) -> {
                        log.warn("Permiso denegado (403 Forbidden): {}", accessDeniedException.getMessage());
                        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                        response.setStatus(HttpStatus.FORBIDDEN.value());
                        Map<String, Object> body = Map.of(
                                "status", HttpStatus.FORBIDDEN.value(),
                                "error", "Forbidden",
                                "message", "No tiene los permisos necesarios para este recurso",
                                "timestamp", LocalDateTime.now().toString()
                        );
                        new ObjectMapper().writeValue(response.getOutputStream(), body);
                    })
            );
        } else {
            // Modo local / desarrollo
            http.addFilterBefore(devMockAuthFilter, UsernamePasswordAuthenticationFilter.class);
        }

        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        if (!azureEnabled) {
            return token -> null;
        }

        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();

        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuerUri);
        OAuth2TokenValidator<Jwt> audienceValidator = new AudienceValidator(clientId, appIdUri);
        OAuth2TokenValidator<Jwt> combinedValidator = new DelegatingOAuth2TokenValidator<>(withIssuer, audienceValidator);

        jwtDecoder.setJwtValidator(combinedValidator);
        return jwtDecoder;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With", "Accept", "Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers", "X-Dev-User-Role", "X-Dev-User-Email"));
        configuration.setExposedHeaders(List.of("Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
