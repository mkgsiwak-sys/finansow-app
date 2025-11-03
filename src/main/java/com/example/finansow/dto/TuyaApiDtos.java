package com.example.finansow.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Kontener na wszystkie rekordy DTO używane do komunikacji z Tuya API.
 */
public final class TuyaApiDtos {

    // --- Token ---

    public record TuyaToken(
            String accessToken,
            String refreshToken,
            long expiresAtMillis,
            String uid
    ) {
        public boolean isExpired() {
            long safetyMargin = 60 * 1000; // 60 sekund
            return System.currentTimeMillis() > (expiresAtMillis - safetyMargin);
        }
    }

    public record TuyaTokenResult(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("refresh_token") String refreshToken,
            @JsonProperty("expires_in") int expiresIn,
            String uid
    ) {}

    public record TuyaTokenResponse(
            TuyaTokenResult result,
            boolean success,
            long t,
            String tid,
            Integer code,
            String msg
    ) {}


    // --- Urządzenia (Devices) ---

    /**
     * DTO dla pierwotnej odpowiedzi z /v2.0/cloud/thing/device
     * Zawiera nieaktualny status 'online'.
     */
    public record TuyaDevice(
            String id,
            String name,
            String productName,
            boolean online, // Ten status jest często nieaktualny!
            String model,
            String ip,
            String localKey,
            long activeTime,
            long createTime,
            long updateTime
    ) {}

    /**
     * Result wrapper zwracany przez endpoint /v2.0/cloud/thing/device.
     * Zawiera paginowaną listę urządzeń w polu "list".
     */
    public record TuyaDeviceListResult(
            List<TuyaDevice> list,
            Integer total,
            Integer size,
            Integer current,
            @JsonProperty("has_more") Boolean hasMore,
            @JsonProperty("next_row_key") String nextRowKey
    ) {}

    /**
     * Główna odpowiedź API podczas pobierania listy urządzeń v2.0.
     */
    public record TuyaDeviceListResponse(
            TuyaDeviceListResult result,
            boolean success,
            long t,
            String tid,
            Integer code,
            String msg
    ) {}

    // --- Status Urządzenia (Pojedynczy) ---

    /**
     * Reprezentacja pojedynczego statusu (np. "switch_1": true)
     */
    public record TuyaDeviceStatus(
            String code,
            Object value // Może być boolean, int, string itp.
    ) {}

    /**
     * DTO dla odpowiedzi z endpointu /v1.0/devices/{id}/status (pojedyncze urządzenie)
     */
    public record TuyaDeviceStatusResponse(
            List<TuyaDeviceStatus> result,
            boolean success,
            long t,
            String tid,
            Integer code,
            String msg
    ) {}


    // ===================================================================
    // === NOWE REKORDY DLA POPRAWKI STATUSU "ONLINE" (Wersja 2) ===
    // ===================================================================

    /**
     * NOWY: Scalony obiekt, który wysyłamy do frontendu.
     * Zawiera nazwę z v2.0 oraz realny status 'online' i 'status' z v1.0.
     */
    public record TuyaDeviceMerged(
            String id,
            String name,
            String productName,
            boolean online, // Poprawiony, realny status
            List<TuyaDeviceStatus> status // Lista statusów, np. switch_1
    ) {}

    /**
     * NOWY: Główna odpowiedź, którą /api/tuya/devices wysyła teraz do frontendu.
     */
    public record TuyaDeviceMergedListResponse(
            List<TuyaDeviceMerged> result,
            boolean success,
            long t,
            String tid,
            Integer code,
            String msg
    ) {}

    // ===================================================================


    // --- Komendy ---

    public record TuyaCommand(
            String code,
            Object value
    ) {}

    public record TuyaCommandRequest(
            List<TuyaCommand> commands
    ) {}

    public record TuyaCommandResponse(
            boolean result,
            boolean success,
            long t,
            String tid,
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