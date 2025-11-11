package com.example.finansow.api;

// Dodałem ten import na podstawie Twojego drugiego pliku
import com.example.finansow.service.TuyaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tuya")
public class TuyaController {

    private final TuyaService tuyaService;

    public TuyaController(TuyaService tuyaService) {
        this.tuyaService = tuyaService;
    }

    @GetMapping("/devices")
    public ResponseEntity<?> listDevices(
            @RequestParam(required = false) Long personId,
            @RequestParam(name = "size", defaultValue = "100") int pageSize
    ) {
        return ResponseEntity.ok(tuyaService.listDevices(personId, pageSize));
    }

    @GetMapping("/device/{deviceId}")
    public ResponseEntity<?> getDevice(
            @PathVariable String deviceId,
            @RequestParam(required = false) Long personId
    ) {
        return ResponseEntity.ok(tuyaService.getDevice(deviceId, personId));
    }

    @PostMapping("/device/{deviceId}/commands")
    public ResponseEntity<?> sendCommands(
            @PathVariable String deviceId,
            @RequestBody CommandRequest request,
            @RequestParam(required = false) Long personId
    ) {
        if (request == null || request.commands == null || request.commands.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Brak commands"));
        }
        Map<String, Object> res = tuyaService.sendCommands(deviceId, request.commands, personId);
        return ResponseEntity.ok(res);
    }

    public static class CommandRequest {
        private List<Map<String, Object>> commands;
        public List<Map<String, Object>> getCommands() { return commands; }
        public void setCommands(List<Map<String, Object>> commands) { this.commands = commands; }
    }
}