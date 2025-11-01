package com.example.finansow.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Kontener na wszystkie rekordy DTO używane do komunikacji z Tuya API.
 * Używamy @JsonProperty, aby jawnie pokazać mapowanie,
 * chociaż konfiguracja SNAKE_CASE w .properties też by to obsłużyła.
 */
public final class TuyaApiDtos {

    // --- Token ---

    /**
     * Wewnętrzna reprezentacja tokena, używana w serwisie.
     * Przechowuje czas wygaśnięcia jako bezwzględny timestamp.
     *
     * === TUTAJ ZMIANA: Dodano pole 'uid' ===
     */
    public record TuyaToken(
            String accessToken,
            String refreshToken,
            long expiresAtMillis,
            String uid // Przechowujemy UID użytkownika zwrócony przy logowaniu
    ) {
        /**
         * Sprawdza, czy token wygasł (z 60-sekundowym buforem bezpieczeństwa).
         */
        public boolean isExpired() {
            long safetyMargin = 60 * 1000; // 60 sekund
            return System.currentTimeMillis() > (expiresAtMillis - safetyMargin);
        }
    }

    /**
     * Obiekt zagnieżdżony w odpowiedzi na żądanie tokena.
     */
    public record TuyaTokenResult(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("refresh_token") String refreshToken,
            @JsonProperty("expires_in") int expiresIn,
            String uid // Ten UID jest kluczowy
    ) {}

    /**
     * Główna odpowiedź API podczas pobierania tokena.
     *
     * === POPRAWKA: Dodane pola 'code' i 'msg' do przechwytywania błędów ===
     * Te pola będą 'null' przy sukcesie, ale zapełnią się przy błędzie.
     */
    public record TuyaTokenResponse(
            TuyaTokenResult result,
            boolean success,
            long t,
            String tid,
            // Pola do przechwytywania błędów
            Integer code,
            String msg
    ) {}


    // --- Urządzenia (Devices) ---

    /**
     * Reprezentacja pojedynczego urządzenia z listy.
     */
    public record TuyaDevice(
            String id,
            String name,
            String productName, // Zmapowane z product_name przez SNAKE_CASE
            boolean online,
            String model,
            String ip,
            String localKey, // Zmapowane z local_key
            long activeTime,
            long createTime,
            long updateTime
    ) {}

    /**
     * Główna odpowiedź API podczas pobierania listy urządzeń.
     *
     * === POPRAWKA: Dodane pola 'code' i 'msg' do przechwytywania błędów ===
     */
    public record TuyaDeviceListResponse(
            List<TuyaDevice> result,
            boolean success,
            long t,
            String tid,
            // Pola do przechwytywania błędów
            Integer code,
            String msg
    ) {}

    // --- Status Urządzenia ---

    /**
     * Reprezentacja pojedynczego statusu (np. "switch_1": true)
     */
    public record TuyaDeviceStatus(
            String code,
            Object value // Może być boolean, int, string itp.
    ) {}

    /**
     * Główna odpowiedź API podczas pobierania statusu urządzenia.
     *
     * === POPRAWKA: Dodane pola 'code' i 'msg' do przechwytywania błędów ===
     */
    public record TuyaDeviceStatusResponse(
            List<TuyaDeviceStatus> result,
            boolean success,
            long t,
            String tid,
            // Pola do przechwytywania błędów
            Integer code,
            String msg
    ) {}


    // --- Komendy ---

    /**
     * Pojedyncza komenda do wysłania (np. code: "switch_1", value: true)
     */
    public record TuyaCommand(
            String code,
            Object value
    ) {}

    /**
     * Żądanie wysłania komendy (lub wielu komend naraz).
     * To zastępuje Twoją wewnętrzną klasę TuyaCommand w kontrolerze.
     */
    public record TuyaCommandRequest(
            List<TuyaCommand> commands
    ) {}

    /**
     * Odpowiedź na wysłanie komendy (zazwyczaj po prostu "true").
     *
     * === POPRAWKA: Dodane pola 'code' i 'msg' do przechwytywania błędów ===
     */
    public record TuyaCommandResponse(
            boolean result,
            boolean success,
            long t,
            String tid,
            // Pola do przechwytywania błędów
            Integer code,
            String msg
    ) {}

    /**
     * Ogólna odpowiedź błędu z Tuya API.
     */
    public record TuyaErrorResponse(
            int code,
            String msg,
            boolean success,
            long t,
            String tid
    ) {}
}