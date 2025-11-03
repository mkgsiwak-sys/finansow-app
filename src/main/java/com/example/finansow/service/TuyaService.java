package com.example.finansow.service;

import com.example.finansow.config.TuyaConfig;
import com.example.finansow.dto.TuyaApiDtos;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Formatter;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

@Service
@Slf4j
public class TuyaService {

    private final TuyaConfig tuyaConfig;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    // Używamy 'volatile' dla bezpiecznej publikacji tokena między wątkami
    private volatile TuyaApiDtos.TuyaToken currentToken;
    // Lock do synchronizacji odświeżania tokena
    private final ReentrantLock tokenRefreshLock = new ReentrantLock();

    public TuyaService(TuyaConfig tuyaConfig, WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.tuyaConfig = tuyaConfig;
        this.objectMapper = objectMapper;
        this.webClient = webClientBuilder
                .baseUrl(tuyaConfig.baseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * === GŁÓWNA POPRAWKA: Metoda do pobierania urządzeń WRAZ Z REALNYM STATUSEM ===
     * Krok 1: Pobiera listę urządzeń (z /v2.0)
     * Krok 2: Pobiera ich realny status (z /v1.0/devices/{id}/status) RÓWNOLEGLE
     * Krok 3: Scala wyniki i zwraca do frontendu.
     */
    public Mono<TuyaApiDtos.TuyaDeviceMergedListResponse> getDevices() {
        return getValidToken().flatMap(token -> {

            // --- KROK 1: Pobierz listę urządzeń (z mylącym statusem 'online') ---
            String v2Path = "/v2.0/cloud/thing/device?page_size=20";
            log.info("Krok 1: Pobieranie listy urządzeń (v2.0)...");

            Mono<TuyaApiDtos.TuyaDeviceListResponse> deviceListMono = buildSignedRequestV1(token, HttpMethod.GET, v2Path, BodyInserters.empty())
                    .header("sign_version", "2.0")
                    .header("mode", "cors")
                    .retrieve()
                    .onStatus(status -> status.isError(), this::handleError)
                    .bodyToMono(TuyaApiDtos.TuyaDeviceListResponse.class);

            return deviceListMono.flatMap(deviceListResponse -> {
                TuyaApiDtos.TuyaDeviceListResult deviceListResult = deviceListResponse.result();
                List<TuyaApiDtos.TuyaDevice> devices = deviceListResult != null ? deviceListResult.list() : Collections.emptyList();

                if (!deviceListResponse.success() || devices == null || devices.isEmpty()) {
                    log.warn("Krok 1 FAILED. API v2.0 zwróciło błąd lub pustą listę. Kod: {}, Wiadomość: {}",
                            deviceListResponse.code(), deviceListResponse.msg());
                    return Mono.just(new TuyaApiDtos.TuyaDeviceMergedListResponse(
                            Collections.emptyList(), true, System.currentTimeMillis(), deviceListResponse.tid(), null, null
                    ));
                }

                log.info("Krok 1 SUKCES. Pobrane nazwy {} urządzeń.", devices.size());

                // --- KROK 2: Pobierz realny status dla każdego urządzenia RÓWNOLEGLE ---
                log.info("Krok 2: Uruchamiam {} równoległych zapytań o status (v1.0)...", devices.size());

                // Tworzymy strumień (Flux) z listy urządzeń
                return Flux.fromIterable(devices)
                        // Uruchamiamy zapytania na osobnym, równoległym wątku
                        .parallel()
                        .runOn(Schedulers.parallel())
                        // Dla każdego urządzenia z listy 'devices', wywołaj 'getDeviceStatus'
                        .flatMap(device ->
                                // Wywołaj naszą istniejącą, pojedynczą metodę
                                this.getDeviceStatus(device.id())
                                        .map(statusResponse -> {
                                            // KROK 3: Scalanie (dla pojedynczego elementu)
                                            // 'statusResponse.result()' to List<TuyaDeviceStatus>
                                            // 'statusResponse.success()' - status online jest implikowany przez sukces
                                            return new TuyaApiDtos.TuyaDeviceMerged(
                                                    device.id(),
                                                    device.name(),
                                                    device.productName(),
                                                    statusResponse.success(), // Realny status online
                                                    statusResponse.result()   // Realne statusy (np. switch_1)
                                            );
                                        })
                                        // Jeśli pojedyncze zapytanie zawiedzie, stwórz 'offline' obiekt
                                        .onErrorResume(e -> {
                                            log.warn("Nie udało się pobrać statusu dla {}: {}", device.id(), e.getMessage());
                                            return Mono.just(new TuyaApiDtos.TuyaDeviceMerged(
                                                    device.id(),
                                                    device.name(),
                                                    device.productName(),
                                                    false, // Oznacz jako offline
                                                    Collections.emptyList()
                                            ));
                                        })
                        )
                        // Zbierz wszystkie scalone wyniki z powrotem do listy
                        .sequential()
                        .collectList()
                        .map(mergedList -> {
                            log.info("Krok 3 SUKCES. Scalono wyniki dla {} urządzeń.", mergedList.size());
                            return new TuyaApiDtos.TuyaDeviceMergedListResponse(
                                    mergedList, true, System.currentTimeMillis(), "merged-tid", null, null
                            );
                        });
            });
        });
    }


    /**
     * Metoda do pobierania statusu POJEDYNCZEGO urządzenia.
     * Wywołuje: GET /v1.0/devices/{id}/status
     */
    public Mono<TuyaApiDtos.TuyaDeviceStatusResponse> getDeviceStatus(String deviceId) {
        String path = "/v1.0/devices/" + deviceId + "/status";
        return getValidToken().flatMap(token ->
                buildSignedRequestV1(token, HttpMethod.GET, path, BodyInserters.empty())
                        .retrieve()
                        .onStatus(status -> status.isError(), this::handleError)
                        .bodyToMono(TuyaApiDtos.TuyaDeviceStatusResponse.class)
                        .doOnSuccess(response -> {
                            if (!response.success()) {
                                log.warn("Zapytanie o status dla {} zwróciło success=false. Kod: {}", deviceId, response.code());
                            }
                        })
        );
    }

    /**
     * Metoda do wysyłania komend do urządzenia.
     */
    public Mono<TuyaApiDtos.TuyaCommandResponse> sendCommand(String deviceId, TuyaApiDtos.TuyaCommandRequest commandRequest) {
        String path = "/v1.0/devices/" + deviceId + "/commands";

        return getValidToken().flatMap(token ->
                buildSignedRequestV1(token, HttpMethod.POST, path, commandRequest)
                        .retrieve()
                        .onStatus(status -> status.isError(), this::handleError)
                        .bodyToMono(TuyaApiDtos.TuyaCommandResponse.class)
        );
    }

    /**
     * TEST DIAGNOSTYCZNY: Pobiera statystyki urządzeń dla projektu.
     */
    public Mono<Object> getDeviceStatistics() {
        return getValidToken().flatMap(token -> {
            String path = "/v1.0/devices/statistics";
            log.info("Uruchamiam test diagnostyczny v1.0: GET /v1.0/devices/statistics");
            return buildSignedRequestV1(token, HttpMethod.GET, path, BodyInserters.empty())
                    .retrieve()
                    .onStatus(status -> status.isError(), this::handleError)
                    .bodyToMono(Object.class);
        });
    }

    // --- LOGIKA TOKENA (v1.0) ---

    private Mono<TuyaApiDtos.TuyaToken> getValidToken() {
        if (currentToken == null || currentToken.isExpired()) {
            return Mono.defer(this::refreshAndGetToken);
        }
        return Mono.just(currentToken);
    }

    private Mono<TuyaApiDtos.TuyaToken> refreshAndGetToken() {
        tokenRefreshLock.lock();
        try {
            if (currentToken == null || currentToken.isExpired()) {
                log.debug("Token wygasł lub nie istnieje. Rozpoczynam pobieranie nowego tokena (v1.0)...");
                TuyaApiDtos.TuyaTokenResponse response = fetchNewToken().block();
                if (response == null || !response.success() || response.result() == null) {
                    String errorMsg = "Nieznany błąd (response był null)";
                    if (response != null) {
                        errorMsg = "Kod: " + response.code() + ", Wiadomość: " + response.msg();
                        log.error("Nie udało się pobrać tokena Tuya. Powód: {}", errorMsg);
                        log.error("Pełna odpowiedź błędu z Tuya: {}", response);
                    } else {
                        log.error(errorMsg);
                    }
                    throw new RuntimeException("Nie udało się pobrać tokena Tuya. " + errorMsg);
                }
                log.info("Pomyślnie pobrano nowy token Tuya.");
                TuyaApiDtos.TuyaTokenResult result = response.result();
                long expiresAt = System.currentTimeMillis() + (result.expiresIn() * 1000L);
                log.info("Pobrano token dla połączonego konta UID: {}", result.uid());
                this.currentToken = new TuyaApiDtos.TuyaToken(
                        result.accessToken(),
                        result.refreshToken(),
                        expiresAt,
                        result.uid()
                );
            } else {
                log.debug("Token został już odświeżony przez inny wątek.");
            }
            return Mono.just(this.currentToken);
        } catch (Exception e) {
            log.error("Krytyczny błąd podczas odświeżania tokena", e);
            return Mono.error(e);
        } finally {
            tokenRefreshLock.unlock();
        }
    }

    /**
     * Używa "złożonego" podpisu v1.0 (bez tokena) z PEŁNĄ ścieżką.
     * Ta metoda jest POPRAWNA (potwierdzone logami).
     */
    private Mono<TuyaApiDtos.TuyaTokenResponse> fetchNewToken() {
        String pathWithQuery = "/v1.0/token?grant_type=1";
        long t = System.currentTimeMillis();
        String bodyHash = sha256("");
        String stringToSign = buildStringToSignV1(
                "", // Pusty accessToken
                t,
                "GET",
                bodyHash,
                pathWithQuery // Używamy pełnej ścieżki (z query)
        );
        String sign = hmacSha256(stringToSign, tuyaConfig.accessSecret());

        return webClient.get()
                .uri(pathWithQuery)
                .header("client_id", tuyaConfig.accessId())
                .header("t", String.valueOf(t))
                .header("sign", sign)
                .header("sign_method", "HMAC-SHA256")
                .retrieve()
                .onStatus(status -> status.isError(), this::handleError)
                .bodyToMono(TuyaApiDtos.TuyaTokenResponse.class);
    }

    // --- LOGIKA PODPISYWANIA v1.0 (Wspólna dla wszystkich) ---

    /**
     * Główny builder żądań. Działa dla v1.0 i (teraz) dla v2.0.
     * Zawsze używa "złożonego" formatu i PEŁNEJ ścieżki.
     */
    private WebClient.RequestHeadersSpec<?> buildSignedRequestV1(
            TuyaApiDtos.TuyaToken token, HttpMethod method,
            String pathWithQuery, BodyInserters.FormInserter<?> emptyBody
    ) {
        long t = System.currentTimeMillis();
        String bodyHash = sha256("");
        String stringToSign = buildStringToSignV1(
                token.accessToken(), t, method.name(), bodyHash, pathWithQuery
        );
        String sign = hmacSha256(stringToSign, tuyaConfig.accessSecret());

        return webClient.method(method)
                .uri(pathWithQuery)
                .header("client_id", tuyaConfig.accessId())
                .header("access_token", token.accessToken())
                .header("t", String.valueOf(t))
                .header("sign", sign)
                .header("sign_method", "HMAC-SHA256")
                .body(emptyBody);
    }

    private WebClient.RequestHeadersSpec<?> buildSignedRequestV1(
            TuyaApiDtos.TuyaToken token, HttpMethod method,
            String path, Object body
    ) {
        long t = System.currentTimeMillis();
        String bodyString;
        try {
            bodyString = (body == null) ? "" : objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Błąd serializacji body do podpisu", e);
        }
        String bodyHash = sha256(bodyString);
        String stringToSign = buildStringToSignV1(
                token.accessToken(), t, method.name(), bodyHash, path
        );
        String sign = hmacSha256(stringToSign, tuyaConfig.accessSecret());

        return webClient.method(method)
                .uri(path)
                .header("client_id", tuyaConfig.accessId())
                .header("access_token", token.accessToken())
                .header("t", String.valueOf(t))
                .header("sign", sign)
                .header("sign_method", "HMAC-SHA256")
                .body(BodyInserters.fromValue(body));
    }


    /**
     * Główna metoda podpisu. Używamy jej do WSZYSTKIEGO.
     * Zawsze używa "złożonego" formatu i PEŁNEJ ścieżki.
     */
    private String buildStringToSignV1(String accessToken, long t, String method, String bodyHash, String path) {
        String stringToSign = tuyaConfig.accessId() +
                accessToken +
                t +
                method.toUpperCase() + "\n" +
                bodyHash + "\n" +
                "\n" +
                path;

        String logString = stringToSign;
        if (accessToken != null && !accessToken.isEmpty()) {
            logString = logString.replace(accessToken, "TOKEN_HIDDEN");
        }
        // Dodajemy rozróżnienie v1/v2 do logu dla jasności
        String logPrefix = path.startsWith("/v2.0") ? "(v2.0)" : "(v1.0)";
        log.trace("StringToSign {}: {}", logPrefix, logString);

        return stringToSign;
    }

    // --- NARZĘDZIA KRYPTOGRAFICZNE (Wspólne) ---

    private String hmacSha256(String data, String key) {
        try {
            Mac sha256Hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256Hmac.init(secretKey);
            byte[] hash = sha256Hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash).toUpperCase();
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Błąd podczas obliczania HMAC-SHA256", e);
            throw new RuntimeException("Błąd kryptografii (HMAC)", e);
        }
    }

    private String sha256(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash).toLowerCase();
        } catch (NoSuchAlgorithmException e) {
            log.error("Błąd podczas obliczania SHA-256", e);
            throw new RuntimeException("Błąd kryptografii (SHA-256)", e);
        }
    }

    private String bytesToHex(byte[] hash) {
        try (Formatter formatter = new Formatter()) {
            for (byte b : hash) {
                formatter.format("%02x", b);
            }
            return formatter.toString();
        }
    }

    private Mono<? extends Throwable> handleError(ClientResponse response) {
        return response.bodyToMono(TuyaApiDtos.TuyaErrorResponse.class)
                .map(errorBody -> {
                    log.error("Błąd API Tuya. Status: {}. Kod: {}. Wiadomość: {}",
                            response.statusCode(), errorBody.code(), errorBody.msg());
                    if (errorBody.code() == 1010) { // 1010 = "token invalid"
                        log.warn("Token jest nieważny (1010). Wymuszam odświeżenie przy następnym żądaniu.");
                        this.currentToken = null; // Unieważnienie tokena
                    }
                    return new RuntimeException("Błąd API Tuya: " + errorBody.msg() + " (Kod: " + errorBody.code() + ")");
                })
                .onErrorResume(e -> {
                    log.error("Błąd API Tuya. Status: {}. Nie można sparsować body błędu.", response.statusCode(), e);
                    return Mono.just(new RuntimeException("Błąd API Tuya (status: " + response.statusCode() + ", nie można sparsować błędu)"));
                });
    }
}