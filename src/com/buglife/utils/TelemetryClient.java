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

public class TelemetryClient {
    private static final Logger logger = LoggerFactory.getLogger(TelemetryClient.class);

    private static final String OVERSEER_HOST = "http://127.0.0.1:8090";
    private static final int TIMEOUT_MS = 2000;
    
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
    
    public static void initialize(String playerId) {
        if (initialized) {
            logger.info("[Telemetry] Already initialized");
            return;
        }
        
        userId = playerId;
        sessionId = UUID.randomUUID().toString();
        executor = Executors.newSingleThreadExecutor();
        initialized = true;
        
        String osInfo = System.getProperty("os.name") + " " + System.getProperty("os.version");
        String payload = String.format(
            "{\"session_id\":\"%s\",\"user_id\":\"%s\",\"os_info\":\"%s\"}",
            sessionId, userId, osInfo
        );
        
        sendAsync("/session/start", payload);
        logger.info("[Telemetry] Session started: {}", sessionId);
    }
    
    public static void shutdown() {
        if (!initialized) return;
        
        String payload = String.format("{\"session_id\":\"%s\"}", sessionId);
        sendSync("/session/end", payload);
        
        if (executor != null) {
            executor.shutdown();
        }
        
        initialized = false;
        logger.info("[Telemetry] Session ended");
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
        
        executor.submit(() -> sendSync(endpoint, jsonPayload));
    }
    
    private static void sendSync(String endpoint, String jsonPayload) {
        HttpURLConnection conn = null;
        try {
            System.out.println("\n[TELEMETRY DEBUG] Attempting to contact Overseer at: " + OVERSEER_HOST + endpoint);
            
            URL url = new URL(OVERSEER_HOST + endpoint);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            System.out.println("[TELEMETRY DEBUG] Server responded with code: " + responseCode);
            
        } catch (Exception e) {
            System.err.println("\n=========================================");
            System.err.println("[FATAL TELEMETRY ERROR] Network connection failed!");
            System.err.println("Exact Reason: " + e.getMessage());
            e.printStackTrace(); 
            System.err.println("=========================================\n");
        } finally {
            if (conn != null) conn.disconnect();
        }
    }
    
    public static String getSessionId() {
        return sessionId;
    }
    
    public static boolean isActive() {
        return initialized;
    }
}
