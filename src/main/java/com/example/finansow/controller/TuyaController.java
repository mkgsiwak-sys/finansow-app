package com.example.finansow.controller;

import com.example.finansow.dto.TuyaApiDtos;
import com.example.finansow.service.TuyaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/tuya") // Utrzymujemy Twój istniejący prefix API
@RequiredArgsConstructor // Zastępuje @Autowired konstruktorem
public class TuyaController {

    private final TuyaService tuyaService;

    /**
     * Endpoint do pobierania listy urządzeń.
     */
    @GetMapping("/devices")
    public Mono<ResponseEntity<TuyaApiDtos.TuyaDeviceListResponse>> getDevices() {
        return tuyaService.getDevices()
                .map(ResponseEntity::ok)
                .onErrorResume(e -> Mono.just(
                        ResponseEntity.status(500).body(null) // Prosta obsługa błędów
                ));
    }

    /**
     * Endpoint do pobierania statusu pojedynczego urządzenia.
     */
    @GetMapping("/devices/{deviceId}/status")
    public Mono<ResponseEntity<TuyaApiDtos.TuyaDeviceStatusResponse>> getDeviceStatus(
            @PathVariable String deviceId
    ) {
        return tuyaService.getDeviceStatus(deviceId)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> Mono.just(
                        ResponseEntity.status(500).body(null)
                ));
    }


    /**
     * Endpoint do wysyłania poleceń (np. włącz/wyłącz).
     * Używa teraz nowego DTO TuyaCommandRequest.
     * Przykładowe body:
     * {
     * "commands": [
     * {
     * "code": "switch_1",
     * "value": true
     * }
     * ]
     * }
     */
    @PostMapping("/devices/{deviceId}/command")
    public Mono<ResponseEntity<TuyaApiDtos.TuyaCommandResponse>> sendCommand(
            @PathVariable String deviceId,
            @RequestBody TuyaApiDtos.TuyaCommandRequest commandRequest // Używamy DTO z TuyaApiDtos
    ) {
        return tuyaService.sendCommand(deviceId, commandRequest)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> Mono.just(
                        ResponseEntity.status(500).body(null)
                ));
    }

    // ===================================================================
    // === NOWY ENDPOINT DIAGNOSTYCZNY ===
    // ===================================================================

    /**
     * TEST DIAGNOSTYCZNY: Endpoint do pobierania statystyk
     */
    @GetMapping("/statistics")
    public Mono<ResponseEntity<Object>> getStatistics() {
        return tuyaService.getDeviceStatistics()
                .map(ResponseEntity::ok)
                .onErrorResume(e -> Mono.just(
                        // Zwracamy treść błędu, aby ją widzieć
                        ResponseEntity.status(500).body(e.getMessage())
                ));
    }
}