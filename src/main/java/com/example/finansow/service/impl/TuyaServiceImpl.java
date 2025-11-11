package com.example.finansow.service.impl;

import com.example.finansow.config.TuyaConfig;
import com.example.finansow.service.TuyaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.*;

@Service
public class TuyaServiceImpl implements TuyaService {

    private final RestTemplate restTemplate;
    private final ObjectMapper om = new ObjectMapper();

    private final String baseUrl;
    private final String clientId;
    private final String clientSecret;

    private volatile TuyaToken cachedToken;
    private final Object tokenLock = new Object();

    public TuyaServiceImpl(RestTemplateBuilder builder, TuyaConfig cfg) {
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(20))
                .build();
        this.baseUrl = (cfg.baseUrl() == null || cfg.baseUrl().isBlank())
                ? "https://openapi.tuyaeu.com"
                : cfg.baseUrl().trim();
        this.clientId = Objects.requireNonNull(cfg.accessId(), "Brak tuya.access-id w konfiguracji");
        this.clientSecret = Objects.requireNonNull(cfg.accessSecret(), "Brak tuya.access-secret w konfiguracji");
    }

    // ================== PUBLIC API ==================

    @Override
    public List<Map<String, Object>> listDevices(Long personId, int pageSize) {
        TuyaToken token = ensureToken();
        String uid = token.uid;
        int pageNo = 1;

        List<Map<String, Object>> all = new ArrayList<>();
        while (true) {
            String path = "/v1.0/users/" + urlEncode(uid) + "/devices?page_no=" + pageNo + "&page_size=" + pageSize;
            Map<String, Object> resp = callTuya(path, HttpMethod.GET, null, token);
            Map<String, Object> result = asMap(resp.get("result"));
            List<Map<String, Object>> devices = asList(result.get("devices"));
            if (devices != null) all.addAll(devices);

            boolean hasMore = result.get("has_more") instanceof Boolean b && b;
            if (!hasMore || devices == null || devices.isEmpty()) break;
            pageNo++;
        }
        return all;
    }

    @Override
    public Map<String, Object> getDevice(String deviceId, Long personId) {
        TuyaToken token = ensureToken();
        String path = "/v1.0/devices/" + urlEncode(deviceId);
        return callTuya(path, HttpMethod.GET, null, token);
    }

    @Override
    public Map<String, Object> sendCommands(String deviceId, List<Map<String, Object>> commands, Long personId) {
        TuyaToken token = ensureToken();
        String path = "/v1.0/devices/" + urlEncode(deviceId) + "/commands";
        Map<String, Object> body = Map.of("commands", commands);
        return callTuya(path, HttpMethod.POST, body, token);
    }

    // ================== TOKEN ==================

    private TuyaToken ensureToken() {
        TuyaToken t = cachedToken;
        long now = System.currentTimeMillis();
        if (t != null && t.expiresAtMs - 60_000 > now) { // bufor 60s
            return t;
        }
        synchronized (tokenLock) {
            t = cachedToken;
            if (t != null && t.expiresAtMs - 60_000 > now) return t;
            TuyaToken fresh = fetchToken();
            cachedToken = fresh;
            return fresh;
        }
    }

    private TuyaToken fetchToken() {
        String path = "/v1.0/token?grant_type=1";
        Map<String, Object> resp = callTuya(path, HttpMethod.GET, null, null);
        if (!Boolean.TRUE.equals(resp.get("success"))) {
            throw new IllegalStateException("Tuya token error: " + resp);
        }
        Map<String, Object> result = asMap(resp.get("result"));
        String access = str(result.get("access_token"));
        String refresh = str(result.get("refresh_token"));
        Number expiresIn = (Number) (result.getOrDefault("expire_time", result.get("expires_in")));
        String uid = str(result.get("uid"));
        long expiresAt = System.currentTimeMillis() + (expiresIn != null ? expiresIn.longValue() * 1000L : 3600_000L);
        return new TuyaToken(access, refresh, uid, expiresAt);
    }

    // ================== LOW LEVEL CALL ==================

    private Map<String, Object> callTuya(String pathAndQuery, HttpMethod method, Object body, TuyaToken tokenOrNull) {
        try {
            String url = baseUrl + pathAndQuery;
            String accessToken = (tokenOrNull != null) ? tokenOrNull.accessToken : "";

            String jsonBody = (body == null) ? "" : om.writeValueAsString(body);
            String contentSHA256 = sha256Hex(jsonBody);

            String stringToSign = method.name() + "\n" + contentSHA256 + "\n" + "\n" + pathAndQuery;

            String nonce = UUID.randomUUID().toString().replace("-", "");
            String t = String.valueOf(System.currentTimeMillis());

            String signStr = clientId + accessToken + t + nonce + stringToSign;
            String sign = hmacSha256Upper(clientSecret, signStr);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("client_id", clientId);
            headers.set("sign", sign);
            headers.set("t", t);
            headers.set("sign_method", "HMAC-SHA256");
            headers.set("nonce", nonce);
            headers.set("lang", "pl");
            if (!accessToken.isEmpty()) headers.set("access_token", accessToken);

            HttpEntity<String> entity = jsonBody.isEmpty()
                    ? new HttpEntity<>("", headers)
                    : new HttpEntity<>(jsonBody, headers);

            ResponseEntity<String> res = restTemplate.exchange(URI.create(url), method, entity, String.class);
            String json = res.getBody();
            if (json == null || json.isBlank()) return Collections.emptyMap();

            @SuppressWarnings("unchecked")
            Map<String, Object> map = om.readValue(json, Map.class);
            return map;
        } catch (Exception e) {
            throw new RuntimeException("Tuya API error: " + e.getMessage(), e);
        }
    }

    // ================== HELPERY ==================

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object o) {
        if (o instanceof Map<?, ?> m) return (Map<String, Object>) m;
        return new HashMap<>();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> asList(Object o) {
        if (o instanceof List<?> list) {
            List<Map<String, Object>> out = new ArrayList<>();
            for (Object el : list) if (el instanceof Map<?, ?> m) out.add((Map<String, Object>) m);
            return out;
        }
        return Collections.emptyList();
    }

    private static String str(Object o){ return o==null?null:String.valueOf(o); }

    private static String sha256Hex(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] dig = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : dig) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String hmacSha256Upper(String secret, String str) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hmac = mac.doFinal(str.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hmac) sb.append(String.format("%02x", b));
            return sb.toString().toUpperCase(Locale.ROOT);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String urlEncode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static final class TuyaToken {
        final String accessToken;
        final String refreshToken;
        final String uid;
        final long expiresAtMs;
        TuyaToken(String accessToken, String refreshToken, String uid, long expiresAtMs) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.uid = uid;
            this.expiresAtMs = expiresAtMs;
        }
    }
}
