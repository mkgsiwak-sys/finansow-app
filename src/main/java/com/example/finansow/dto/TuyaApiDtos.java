package com.example.finansow.dto;

import java.util.List;
import java.util.Map;

public final class TuyaApiDtos {

    // --- TOKEN ---
    public record TuyaTokenResponse(boolean success, long t, String tid, Integer code, String msg,
                                    TuyaTokenResult result) {}
    public record TuyaTokenResult(String accessToken, String refreshToken, long expiresIn, String uid) {}

    public static final class TuyaToken {
        private final String accessToken;
        private final String refreshToken;
        private final long expiresAtMs;
        private final String uid;

        public TuyaToken(String accessToken, String refreshToken, long expiresAtMs, String uid) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.expiresAtMs = expiresAtMs;
            this.uid = uid;
        }
        public String accessToken() { return accessToken; }
        public String refreshToken() { return refreshToken; }
        public String uid() { return uid; }
        public boolean isExpired() { return System.currentTimeMillis() > (expiresAtMs - 60_000L); }
    }

    public record TuyaErrorResponse(Integer code, String msg) {}

    // --- DEVICES / STATUS / COMMANDS ---
    public record TuyaDevice(
            String id, String name, String productName, boolean online,
            String model, String ip, String localKey,
            long activeTime, long createTime, long updateTime
    ) {}

    public record TuyaDeviceStatusResponse(
            boolean success, Integer code, String msg, List<Map<String, Object>> result
    ) {}

    public record TuyaCommandRequest(List<Map<String, Object>> commands) {}
    public record TuyaCommandResponse(boolean success, Integer code, String msg, Object result) {}

    // --- MERGED DTO for UI ---
    public record TuyaDeviceMerged(String id, String name, String productName, boolean success, Object result) {}
    public record TuyaDeviceMergedListResponse(
            List<TuyaDeviceMerged> merged, boolean success, long t, String tid, Integer code, String msg
    ) {}
}
