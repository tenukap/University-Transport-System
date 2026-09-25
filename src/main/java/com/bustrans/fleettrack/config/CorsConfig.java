package com.bustrans.fleettrack.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Global, development-only CORS configuration for every endpoint.
 *
 * <p>Exposed as a {@link CorsConfigurationSource} bean so it is picked up by both
 * Spring MVC and Spring Security's {@code http.cors(...)} filter chain
 * (see SecurityConfig), meaning preflight requests to secured endpoints are
 * handled correctly.</p>
 *
 * <p>NOTE: {@code allowedOriginPatterns("*")} is used instead of
 * {@code allowedOrigins("*")} because the CORS spec forbids the wildcard origin
 * together with {@code allowCredentials(true)} — Spring would throw at runtime.
 * Tighten these values before any production deployment.</p>
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*", "null")); // any origin including file:///
        config.setAllowedMethods(List.of("*"));        // GET, POST, PUT, DELETE, OPTIONS, ...
        config.setAllowedHeaders(List.of("*"));        // any request header
        config.setExposedHeaders(List.of("*"));        // expose any response header
        config.setAllowCredentials(true);              // allow cookies / Authorization

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
