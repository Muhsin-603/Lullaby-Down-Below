package com.buglife.utils;

import java.net.HttpURLConnection;
import java.net.URL;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class TelemetryClient {
    private static final Logger logger = LoggerFactory.getLogger(TelemetryClient.class);

    private static final String OVERSEER_HOST = "http://127.0.0.1:8090";
    private static final int TIMEOUT_MS = 2000;
    private static final ObjectMapper mapper = new ObjectMapper();
    
    public static final String EVENT_STEALTH_BROKEN = "STEALTH_BROKEN";
    public static final String EVENT_PLAYER_DEATH = "PLAYER_DEATH";
    public static final String EVENT_ITEM_USED = "ITEM_USED";
    public static final String EVENT_LEVEL_COMPLETE = "LEVEL_COMPLETE";
    public static final String EVENT_ENEMY_ALERT = "ENEMY_ALERT";
    public static final String EVENT_CHECKPOINT = "CHECKPOINT";
    public static final String EVENT_DAMAGE_TAKEN = "DAMAGE_TAKEN";
    
    private static String sessionId = null;
    private static String userId = null;
    private static ExecutorService executor = null;
    private static boolean initialized = false;
    private static long startTimeMillis = 0;
    private static long basePlaytimeSeconds = 0; // Cumulative time from previous sessions
    
    public static void initialize(String playerId) {
        initialize(playerId, 0);
    }

    public static void initialize(String playerId, long startingTotalPlaytime) {
        if (initialized) {
            logger.info("[Telemetry] Already initialized, re-initializing with new player: {}", playerId);
            shutdown();
        }
        
        userId = playerId;
        sessionId = UUID.randomUUID().toString();
        executor = Executors.newSingleThreadExecutor();
        initialized = true;
        startTimeMillis = System.currentTimeMillis();
        basePlaytimeSeconds = startingTotalPlaytime;
        
        String osInfo = System.getProperty("os.name") + " " + System.getProperty("os.version");
        String payload = String.format(
            "{\"session_id\":\"%s\",\"user_id\":\"%s\",\"os_info\":\"%s\",\"starting_total_playtime\":%d}",
            sessionId, userId, osInfo, basePlaytimeSeconds
        );
        
        sendAsync("/session/start", payload);
        logger.info("[Telemetry] Session started: {} (Base playtime: {}s)", sessionId, basePlaytimeSeconds);
    }
    
    /**
     * Format playtime into human-readable string (e.g. "1h 23m 45s")
     */
    public static String formatPlaytime(long totalSeconds) {
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        
        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }
    
    public static void shutdown() {
        if (!initialized) return;
        
        long sessionSeconds = (System.currentTimeMillis() - startTimeMillis) / 1000;
        long totalPlaytimeSeconds = basePlaytimeSeconds + sessionSeconds;
        
        String payload = String.format(
            "{\"session_id\":\"%s\",\"playtime_seconds\":%d,\"total_playtime_seconds\":%d}", 
            sessionId, sessionSeconds, totalPlaytimeSeconds
        );
        sendSync("/session/end", payload);
        
        if (executor != null) {
            executor.shutdown();
        }
        
        initialized = false;
        logger.info("[Telemetry] Session ended. Session: {}s, Total: {}s", sessionSeconds, totalPlaytimeSeconds);
    }

    
    
    public static void onStealthBroken(float x, float y, String enemyType) {
        sendEvent(EVENT_STEALTH_BROKEN, x, y, 
            String.format("{\"enemy_type\":\"%s\"}", enemyType));
    }
    
    public static void onPlayerDeath(float x, float y, String causeOfDeath) {
        sendEvent(EVENT_PLAYER_DEATH, x, y,
            String.format("{\"cause\":\"%s\"}", causeOfDeath));
    }
    
    public static void onItemUsed(float x, float y, String itemType) {
        sendEvent(EVENT_ITEM_USED, x, y,
            String.format("{\"item_type\":\"%s\"}", itemType));
    }
    
    public static void onLevelComplete(float x, float y, String levelName, int timeSeconds) {
        sendEvent(EVENT_LEVEL_COMPLETE, x, y,
            String.format("{\"level\":\"%s\",\"time_seconds\":%d}", levelName, timeSeconds));
    }
    
    public static void onEnemyAlert(float x, float y, String enemyType) {
        sendEvent(EVENT_ENEMY_ALERT, x, y,
            String.format("{\"enemy_type\":\"%s\"}", enemyType));
    }
    
    public static void onCheckpoint(float x, float y, String checkpointId) {
        sendEvent(EVENT_CHECKPOINT, x, y,
            String.format("{\"checkpoint_id\":\"%s\"}", checkpointId));
    }
    
    public static void onDamageTaken(float x, float y, int damageAmount, String source) {
        sendEvent(EVENT_DAMAGE_TAKEN, x, y,
            String.format("{\"damage\":%d,\"source\":\"%s\"}", damageAmount, source));
    }
    
    private static void sendEvent(String eventType, float x, float y, String metaJson) {
        if (!initialized) return;
        
        // CRITICAL: java.util.Locale.US forces decimals instead of commas!
        String payload = String.format(java.util.Locale.US,
            "{\"session_id\":\"%s\",\"event_type\":\"%s\",\"x\":%.2f,\"y\":%.2f,\"meta\":%s}",
            sessionId, eventType, x, y, metaJson
        );
        
        System.out.println("[TELEMETRY DEBUG] Packaging Event: " + eventType);
        sendAsync("/event", payload);
    }
    
    private static void sendAsync(String endpoint, String jsonPayload) {
        if (executor == null || executor.isShutdown()) return;
        
        executor.submit(() -> {
            String response = sendSync(endpoint, jsonPayload);
            
            // If starting a session, synchronize our basePlaytime with the server's authoritative value
            if (endpoint.equals("/session/start") && response != null) {
                try {
                    JsonNode root = mapper.readTree(response);
                    if (root.has("total_playtime")) {
                        long serverPlaytime = root.get("total_playtime").asLong();
                        if (serverPlaytime > basePlaytimeSeconds) {
                            logger.info("[Telemetry] Server has higher total playtime: {}s (Server) vs {}s (Local). Syncing...", 
                                serverPlaytime, basePlaytimeSeconds);
                            basePlaytimeSeconds = serverPlaytime;
                        }
                    }
                } catch (Exception e) {
                    logger.warn("[Telemetry] Failed to parse session/start response: {}", e.getMessage());
                }
            }
        });
    }
    
    private static String sendSync(String endpoint, String jsonPayload) {
        HttpURLConnection conn = null;
        try {
            System.out.println("\n[TELEMETRY DEBUG] Attempting to contact Overseer at: " + OVERSEER_HOST + endpoint);
            
            URL url = new URL(OVERSEER_HOST + endpoint);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            System.out.println("[TELEMETRY DEBUG] Server responded with code: " + responseCode);
            
            if (responseCode >= 200 && responseCode < 300) {
                StringBuilder response = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                }
                return response.toString();
            }
            
        } catch (Exception e) {
            System.err.println("\n=========================================");
            System.err.println("[FATAL TELEMETRY ERROR] Network connection failed!");
            System.err.println("Exact Reason: " + e.getMessage());
            e.printStackTrace(); 
            System.err.println("=========================================\n");
        } finally {
            if (conn != null) conn.disconnect();
        }
        return null;
    }
    
    public static String getUserId() {
        return userId;
    }

    public static String getSessionId() {
        return sessionId;
    }
    
    public static boolean isActive() {
        return initialized;
    }

    public static long getCurrentSessionSeconds() {
        if (!initialized) return 0;
        return (System.currentTimeMillis() - startTimeMillis) / 1000;
    }

    public static long getTotalPlaytimeSeconds() {
        if (!initialized) return 0;
        return basePlaytimeSeconds + getCurrentSessionSeconds();
    }
}
