package com.example.finansow.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Klasa konfiguracyjna (rekord) mapująca właściwości z application.properties
 * o prefiksie "tuya".
 * Używa walidacji, aby upewnić się, że kluczowe wartości są ustawione.
 */
@ConfigurationProperties(prefix = "tuya")
@Validated
public record TuyaConfig(
        @NotBlank String accessId,
        @NotBlank String accessSecret,
        @NotBlank String baseUrl
) {
}