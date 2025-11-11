package com.example.finansow.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Wczytywane z application.properties / application.yml pod prefixem "tuya".
 * UWAGA: bez @Configuration i bez final class.
 * Rekord jest wspierany w Spring Boot 3.
 */
@ConfigurationProperties(prefix = "tuya")
public record TuyaConfig(
        String baseUrl,
        String accessId,
        String accessSecret
) {}
