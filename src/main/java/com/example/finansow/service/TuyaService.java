package com.example.finansow.service;

import java.util.List;
import java.util.Map;

public interface TuyaService {
    List<Map<String, Object>> listDevices(Long personId, int pageSize);
    Map<String, Object> getDevice(String deviceId, Long personId);
    Map<String, Object> sendCommands(String deviceId, List<Map<String, Object>> commands, Long personId);
}
