package com.buglife.telemetry;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.buglife.config.ConfigManager;

/**
 * MongoTelemetryClient — The Overseer Link.
 * 
 * Sends telemetry data to the Overseer HTTP server (Telemetry-System backend)
 * which persists everything to MongoDB. Runs in parallel with the SQL-based
 * TelemetryClient so both MySQL and MongoDB receive telemetry data.
 * 
 * All writes are asynchronous. Deaths and events are batched (15 per flush)
 * to reduce HTTP overhead. The server must be running for data to be captured;
 * if the Overseer is unreachable, operations fail silently.
 * 
 * Configuration (config.json):
 *   telemetry.overseer.enabled  → true/false (default: false)
 *   telemetry.overseer.host     → server URL (default: http://127.0.0.1)
 *   telemetry.overseer.port     → server port (default: 8090)
 */
public class MongoTelemetryClient {
    private static final Logger logger = LoggerFactory.getLogger(MongoTelemetryClient.class);

    private static MongoTelemetryClient instance;

    // ========================================================================
    // CONFIG
    // ========================================================================
    private final String overseerHost;
    private static final int TIMEOUT_MS = 2000;
    private static final int BATCH_SIZE = 15;

    // ========================================================================
    // STATE
    // ========================================================================
    private final boolean enabled;
    private final ExecutorService executor;
    private volatile boolean initialized = false;

    private volatile int playerId = -1;
    private volatile int sessionId = -1;
    private volatile int saveId = -1;
    private volatile String currentLevel = "level1";

    private final ConcurrentLinkedQueue<String> deathQueue = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<String> eventQueue = new ConcurrentLinkedQueue<>();

    // ========================================================================
    // CONSTRUCTION
    // ========================================================================

    private MongoTelemetryClient() {
        ConfigManager config = ConfigManager.getInstance();

        this.enabled = config.getBoolean("telemetry.overseer.enabled", false);

        if (!enabled) {
            logger.info("Overseer telemetry (MongoDB) disabled in config");
            this.overseerHost = null;
            this.executor = null;
            return;
        }

        String host = config.getString("telemetry.overseer.host", "http://127.0.0.1");
        int port = config.getInt("telemetry.overseer.port", 8090);
        this.overseerHost = host + ":" + port;

        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "Overseer-Writer");
            t.setDaemon(true);
            return t;
        });

        logger.info("Overseer telemetry client initialized — target: {}", overseerHost);
    }

    public static synchronized MongoTelemetryClient getInstance() {
        if (instance == null) {
            instance = new MongoTelemetryClient();
        }
        return instance;
    }

    /**
     * Is the Overseer telemetry channel operational?
     */
    public boolean isOperational() {
        return enabled && overseerHost != null && executor != null && !executor.isShutdown();
    }

    // ========================================================================
    // PLAYER MANAGEMENT
    // ========================================================================

    /**
     * Register (or re-identify) a player with the Overseer.
     * Captures the server-assigned player_id.
     * 
     * @param username The player's name
     */
    public void identifyPlayer(String username) {
        if (!isOperational()) return;

        executor.submit(() -> {
            String payload = String.format(
                    "{\"username\":\"%s\",\"password\":\"\"}",
                    escapeJson(username));

            String response = sendSyncWithResponse("/player/register", payload);
            if (response != null) {
                playerId = extractInt(response, "\"player_id\":");
                logger.debug("Overseer: Player '{}' registered with id={}", username, playerId);
            } else {
                logger.warn("Overseer: Failed to register player '{}'", username);
            }
        });
    }

    // ========================================================================
    // SAVE FILE TRACKING
    // ========================================================================

    /**
     * Upload/update a save file slot for the current player.
     * 
     * @param slotNumber    Save slot index
     * @param completionPct Completion percentage (0.00 to 100.00)
     */
    public void updateSaveFile(int slotNumber, double completionPct) {
        if (!isOperational() || playerId < 0) return;

        executor.submit(() -> {
            String payload = String.format(
                    "{\"player_id\":%d,\"slot_number\":%d,\"completion_pct\":%.2f}",
                    playerId, slotNumber, completionPct);

            String response = sendSyncWithResponse("/save/upload", payload);
            if (response != null) {
                saveId = extractInt(response, "\"save_id\":");
                logger.debug("Overseer: Save slot {} uploaded, save_id={}", slotNumber, saveId);
            }
        });
    }

    // ========================================================================
    // SESSION MANAGEMENT
    // ========================================================================

    /**
     * Start a new telemetry session with the Overseer.
     */
    public void startSession() {
        if (!isOperational() || playerId < 0) return;

        executor.submit(() -> {
            String payload;
            if (saveId >= 0) {
                payload = String.format(
                        "{\"player_id\":%d,\"save_id\":%d}",
                        playerId, saveId);
            } else {
                payload = String.format(
                        "{\"player_id\":%d}",
                        playerId);
            }

            String response = sendSyncWithResponse("/session/start", payload);
            if (response != null) {
                sessionId = extractInt(response, "\"session_id\":");
                initialized = true;
                logger.debug("Overseer: Session started, session_id={}", sessionId);
            }
        });
    }

    /**
     * End the current Overseer session — flushes queued data first.
     */
    public void endSession() {
        if (!isOperational() || sessionId < 0) return;
        final int sid = sessionId;

        executor.submit(() -> {
            // Flush remaining batches before ending the session
            flushDeathQueue();
            flushEventQueue();

            String payload = String.format("{\"session_id\":%d}", sid);
            sendSync("/session/end", payload);
            logger.debug("Overseer: Session {} ended", sid);
        });

        sessionId = -1;
        initialized = false;
    }

    // ========================================================================
    // DEATH TRACKING
    // ========================================================================

    /**
     * Set the current level for area_code resolution.
     */
    public void setCurrentLevel(String levelName) {
        this.currentLevel = levelName;
    }

    /**
     * Queue a death event. Automatically flushed when batch size is reached.
     * 
     * @param deathX     X coordinate at death
     * @param deathY     Y coordinate at death
     * @param deathCause Human-readable cause
     */
    public void recordDeath(double deathX, double deathY, String deathCause) {
        if (!isOperational() || sessionId < 0) return;

        int areaCode = MapZoneManager.computeAreaCode(currentLevel, deathX, deathY);

        String payload;
        if (areaCode >= 0) {
            payload = String.format(
                    "{\"session_id\":%d,\"area_code\":%d,\"death_x\":%.2f,\"death_y\":%.2f,\"death_cause\":\"%s\"}",
                    sessionId, areaCode, deathX, deathY, escapeJson(deathCause));
        } else {
            payload = String.format(
                    "{\"session_id\":%d,\"death_x\":%.2f,\"death_y\":%.2f,\"death_cause\":\"%s\"}",
                    sessionId, deathX, deathY, escapeJson(deathCause));
        }

        deathQueue.add(payload);

        if (deathQueue.size() >= BATCH_SIZE) {
            executor.submit(this::flushDeathQueue);
        }
    }

    // ========================================================================
    // EVENT TRACKING
    // ========================================================================

    /**
     * Queue a generic gameplay event. Flushed when batch size is reached.
     * 
     * @param eventType  Event type (e.g., "FOOD_EATEN", "DASH_USED")
     * @param eventX     X coordinate (nullable)
     * @param eventY     Y coordinate (nullable)
     * @param eventValue Optional integer payload (nullable)
     */
    public void recordEvent(String eventType, Double eventX, Double eventY, Integer eventValue) {
        if (!isOperational() || sessionId < 0) return;

        int areaCode = (eventX != null && eventY != null)
                ? MapZoneManager.computeAreaCode(currentLevel, eventX, eventY)
                : -1;

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("{\"session_id\":%d,\"event_type\":\"%s\"", sessionId, escapeJson(eventType)));
        if (areaCode >= 0) {
            sb.append(String.format(",\"area_code\":%d", areaCode));
        }
        if (eventX != null) {
            sb.append(String.format(",\"event_x\":%.2f", eventX));
        }
        if (eventY != null) {
            sb.append(String.format(",\"event_y\":%.2f", eventY));
        }
        if (eventValue != null && eventValue >= 0) {
            sb.append(String.format(",\"event_value\":%d", eventValue));
        }
        sb.append("}");

        eventQueue.add(sb.toString());

        if (eventQueue.size() >= BATCH_SIZE) {
            executor.submit(this::flushEventQueue);
        }
    }

    /** Convenience: event with position only. */
    public void recordEvent(String eventType, double eventX, double eventY) {
        recordEvent(eventType, eventX, eventY, null);
    }

    /** Convenience: event with type only. */
    public void recordEvent(String eventType) {
        recordEvent(eventType, null, null, null);
    }

    // ========================================================================
    // QUEUE FLUSHING
    // ========================================================================

    private void flushDeathQueue() {
        if (deathQueue.isEmpty()) return;

        StringBuilder batch = new StringBuilder("[");
        String entry;
        boolean first = true;
        while ((entry = deathQueue.poll()) != null) {
            if (!first) batch.append(",");
            batch.append(entry);
            first = false;
        }
        batch.append("]");

        sendSync("/death/batch", batch.toString());
    }

    private void flushEventQueue() {
        if (eventQueue.isEmpty()) return;

        StringBuilder batch = new StringBuilder("[");
        String entry;
        boolean first = true;
        while ((entry = eventQueue.poll()) != null) {
            if (!first) batch.append(",");
            batch.append(entry);
            first = false;
        }
        batch.append("]");

        sendSync("/event/batch", batch.toString());
    }

    // ========================================================================
    // HTTP CORE
    // ========================================================================

    /**
     * POST JSON synchronously; no response captured.
     */
    private void sendSync(String endpoint, String jsonPayload) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(overseerHost + endpoint);
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

            int code = conn.getResponseCode();
            if (code != 200) {
                logger.warn("Overseer: {} returned HTTP {}", endpoint, code);
            }
        } catch (Exception e) {
            logger.debug("Overseer: Send to {} failed: {}", endpoint, e.getMessage());
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    /**
     * POST JSON synchronously; return response body (or null on failure).
     */
    private String sendSyncWithResponse(String endpoint, String jsonPayload) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(overseerHost + endpoint);
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

            int code = conn.getResponseCode();
            if (code == 200) {
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }
                    return response.toString();
                }
            } else {
                logger.warn("Overseer: {} returned HTTP {}", endpoint, code);
            }
        } catch (Exception e) {
            logger.debug("Overseer: Send to {} failed: {}", endpoint, e.getMessage());
        } finally {
            if (conn != null) conn.disconnect();
        }
        return null;
    }

    // ========================================================================
    // UTILITY
    // ========================================================================

    /**
     * Extract an integer value from a JSON response string.
     * Scans for the given key (e.g. "\"player_id\":") and parses the number after it.
     */
    private int extractInt(String json, String key) {
        int idx = json.indexOf(key);
        if (idx < 0) return -1;
        String sub = json.substring(idx + key.length()).trim();
        StringBuilder num = new StringBuilder();
        for (char c : sub.toCharArray()) {
            if (Character.isDigit(c)) num.append(c);
            else break;
        }
        return num.length() > 0 ? Integer.parseInt(num.toString()) : -1;
    }

    /**
     * Escape special characters in a string for safe JSON embedding.
     */
    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    // ========================================================================
    // ACCESSORS
    // ========================================================================

    public int getPlayerId()  { return playerId; }
    public int getSessionId() { return sessionId; }
    public int getSaveId()    { return saveId; }
    public boolean isActive() { return initialized; }

    // ========================================================================
    // LIFECYCLE
    // ========================================================================

    /**
     * Shutdown the Overseer telemetry client gracefully.
     * Flushes remaining events and shuts down the executor.
     */
    public void shutdown() {
        if (executor == null) return;

        // End active session (which flushes queues internally)
        if (sessionId > 0) {
            endSession();
        }

        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                logger.warn("Overseer: Writer thread did not drain in time, forcing shutdown");
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            logger.warn("Overseer: Shutdown interrupted");
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        instance = null;
        logger.info("Overseer telemetry client shutdown complete");
    }
}
