package com.berkan.receiptscanner.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${security.cors.allowed-origins:http://localhost:3000,http://127.0.0.1:3000}") String allowedOrigins,
            @Value("${security.cors.allowed-methods:GET,POST,PUT,PATCH,DELETE,OPTIONS}") String allowedMethods,
            @Value("${security.cors.allowed-headers:*}") String allowedHeaders,
            @Value("${security.cors.exposed-headers:X-RateLimit-Limit,X-RateLimit-Window-Seconds}") String exposedHeaders,
            @Value("${security.cors.allow-credentials:true}") boolean allowCredentials,
            @Value("${security.cors.max-age-seconds:3600}") long maxAgeSeconds) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(parseCsv(allowedOrigins));
        config.setAllowedMethods(parseCsv(allowedMethods));
        config.setAllowedHeaders(parseCsv(allowedHeaders));
        config.setExposedHeaders(parseCsv(exposedHeaders));
        config.setAllowCredentials(allowCredentials);
        config.setMaxAge(maxAgeSeconds);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private static List<String> parseCsv(String csv) {
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toList());
    }
}
