package com.buglife.telemetry;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.buglife.config.ConfigManager;
import com.buglife.telemetry.DatabaseManager;

/**
 * TelemetryClient — The Scribe of the Vault.
 * 
 * Records player sessions, death events, and gameplay events to the
 * telemetry MySQL database. All writes are async (fire-and-forget) so
 * the game loop is never blocked.
 * 
 * Schema tables:
 *   - player         → registered players (username/password)
 *   - savefile        → save slots and completion
 *   - mapzone         → named areas on levels
 *   - playersession   → session start/end times
 *   - deathevent      → where and how the player died
 *   - playerevent     → generic events (food eaten, web escaped, etc.)
 * 
 * If the database is unavailable, all operations fail silently.
 */
public class TelemetryClient {
    private static final Logger logger = LoggerFactory.getLogger(TelemetryClient.class);

    private static TelemetryClient instance;

    private final DatabaseManager db;
    private final ExecutorService executor;
    private final boolean enabled;

    // Cached IDs for the current session
    private volatile int playerId = -1;
    private volatile int sessionId = -1;
    private volatile int saveId = -1;

    // Current level — needed for area_code resolution
    private volatile String currentLevel = "level1";

    // ====================================================================
    // SQL Statements — matching the vault schema exactly
    // ====================================================================

    // player table
    private static final String SQL_FIND_PLAYER =
            "SELECT player_id FROM player WHERE username = ?";
    private static final String SQL_INSERT_PLAYER =
            "INSERT INTO player (username, password) VALUES (?, ?) ON DUPLICATE KEY UPDATE player_id = LAST_INSERT_ID(player_id)";

    // savefile table
    private static final String SQL_FIND_SAVE =
            "SELECT save_id FROM savefile WHERE player_id = ? AND slot_number = ?";
    private static final String SQL_INSERT_SAVE =
            "INSERT INTO savefile (player_id, slot_number, completion_pct) VALUES (?, ?, ?)";
    private static final String SQL_UPDATE_SAVE =
            "UPDATE savefile SET completion_pct = ?, last_updated = CURRENT_TIMESTAMP WHERE save_id = ?";

    // playersession table
    private static final String SQL_START_SESSION =
            "INSERT INTO playersession (player_id, save_id, start_time) VALUES (?, ?, ?)";
    private static final String SQL_END_SESSION =
            "UPDATE playersession SET end_time = ? WHERE session_id = ?";

    // deathevent table
    private static final String SQL_INSERT_DEATH =
            "INSERT INTO deathevent (session_id, area_code, death_x, death_y, death_cause) VALUES (?, ?, ?, ?, ?)";

    // playerevent table
    private static final String SQL_INSERT_EVENT =
            "INSERT INTO playerevent (session_id, area_code, event_type, event_x, event_y, event_value, event_time) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?)";

    // mapzone — area_code is now computed by MapZoneManager, no DB lookup needed

    private TelemetryClient() {
        ConfigManager config = ConfigManager.getInstance();
        this.enabled = config.getInt("telemetry.enabled", 0) == 1 
                || "true".equalsIgnoreCase(config.getString("telemetry.enabled", "false"));
        
        if (!enabled) {
            logger.info("Telemetry disabled in config");
            this.db = null;
            this.executor = null;
            return;
        }

        this.db = DatabaseManager.getInstance();
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "Telemetry-Writer");
            t.setDaemon(true);
            return t;
        });

        if (db.isAvailable()) {
            logger.info("Telemetry client initialized — recording to vault");
            // Populate mapzone grid for all known levels (idempotent)
            executor.submit(() -> MapZoneManager.ensureZonesPopulated(db));
        } else {
            logger.warn("Telemetry client initialized but database unavailable — silent mode");
        }
    }

    public static synchronized TelemetryClient getInstance() {
        if (instance == null) {
            instance = new TelemetryClient();
        }
        return instance;
    }

    /**
     * Is telemetry operational? (enabled in config AND database reachable)
     */
    public boolean isOperational() {
        return enabled && db != null && db.isAvailable();
    }

    // ====================================================================
    // PLAYER MANAGEMENT
    // ====================================================================

    /**
     * Register or find a player by username. Caches the player_id.
     * Called when a player identifies themselves at the start screen.
     * 
     * @param username The player's name (from UserProfile)
     */
    public void identifyPlayer(String username) {
        if (!isOperational()) return;
        executor.submit(() -> {
            try (Connection conn = db.getConnection()) {
                // Try to find existing player first
                try (PreparedStatement ps = conn.prepareStatement(SQL_FIND_PLAYER)) {
                    ps.setString(1, username);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            playerId = rs.getInt("player_id");
                            logger.debug("Telemetry: Found player '{}' with id={}", username, playerId);
                            return;
                        }
                    }
                }
                // Not found — insert with empty password (password managed by Overseer)
                try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT_PLAYER, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, username);
                    ps.setString(2, ""); // Password is handled by the Overseer, not the game client
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) {
                            playerId = rs.getInt(1);
                            logger.debug("Telemetry: Registered new player '{}' with id={}", username, playerId);
                        }
                    }
                }
            } catch (SQLException e) {
                logger.warn("Telemetry: Failed to identify player '{}': {}", username, e.getMessage());
            }
        });
    }

    // ====================================================================
    // SAVE FILE TRACKING
    // ====================================================================

    /**
     * Register or update a save slot for the current player.
     * 
     * @param slotNumber    The save slot (typically 0 for single-slot)
     * @param completionPct Completion percentage (0.00 to 100.00)
     */
    public void updateSaveFile(int slotNumber, double completionPct) {
        if (!isOperational() || playerId < 0) return;
        executor.submit(() -> {
            try (Connection conn = db.getConnection()) {
                // Check if save slot exists
                try (PreparedStatement ps = conn.prepareStatement(SQL_FIND_SAVE)) {
                    ps.setInt(1, playerId);
                    ps.setInt(2, slotNumber);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            saveId = rs.getInt("save_id");
                            // Update existing
                            try (PreparedStatement upd = conn.prepareStatement(SQL_UPDATE_SAVE)) {
                                upd.setDouble(1, completionPct);
                                upd.setInt(2, saveId);
                                upd.executeUpdate();
                            }
                            return;
                        }
                    }
                }
                // Insert new save slot
                try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT_SAVE, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, playerId);
                    ps.setInt(2, slotNumber);
                    ps.setDouble(3, completionPct);
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) {
                            saveId = rs.getInt(1);
                        }
                    }
                }
                logger.debug("Telemetry: Save slot {} updated for player_id={}", slotNumber, playerId);
            } catch (SQLException e) {
                logger.warn("Telemetry: Failed to update save file: {}", e.getMessage());
            }
        });
    }

    // ====================================================================
    // SESSION MANAGEMENT
    // ====================================================================

    /**
     * Start a new gameplay session. Called when entering PlayingState.
     */
    public void startSession() {
        if (!isOperational() || playerId < 0) return;
        executor.submit(() -> {
            try (Connection conn = db.getConnection()) {
                try (PreparedStatement ps = conn.prepareStatement(SQL_START_SESSION, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, playerId);
                    if (saveId > 0) {
                        ps.setInt(2, saveId);
                    } else {
                        ps.setNull(2, java.sql.Types.INTEGER);
                    }
                    ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) {
                            sessionId = rs.getInt(1);
                            logger.debug("Telemetry: Session started, session_id={}", sessionId);
                        }
                    }
                }
            } catch (SQLException e) {
                logger.warn("Telemetry: Failed to start session: {}", e.getMessage());
            }
        });
    }

    /**
     * End the current gameplay session. Called when leaving PlayingState
     * (game over, level complete, quit to menu).
     */
    public void endSession() {
        if (!isOperational() || sessionId < 0) return;
        final int sid = sessionId;
        executor.submit(() -> {
            try (Connection conn = db.getConnection()) {
                try (PreparedStatement ps = conn.prepareStatement(SQL_END_SESSION)) {
                    ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
                    ps.setInt(2, sid);
                    ps.executeUpdate();
                    logger.debug("Telemetry: Session {} ended", sid);
                }
            } catch (SQLException e) {
                logger.warn("Telemetry: Failed to end session: {}", e.getMessage());
            }
        });
        sessionId = -1;
    }

    // ====================================================================
    // DEATH EVENTS
    // ====================================================================

    /**
     * Set the current level — must be called when the level changes
     * so area_code is resolved correctly.
     * 
     * @param levelName e.g. "level1", "level2"
     */
    public void setCurrentLevel(String levelName) {
        this.currentLevel = levelName;
    }

    /**
     * Record a death event. Called when the player dies from any cause.
     * 
     * @param deathX    Player X coordinate at time of death
     * @param deathY    Player Y coordinate at time of death
     * @param deathCause Human-readable cause: "WEB_DEATH", "SPIDER_ZERO_HUNGER",
     *                   "SPIDER_CRYING", "STARVATION"
     */
    public void recordDeath(double deathX, double deathY, String deathCause) {
        if (!isOperational() || sessionId < 0) return;
        final int sid = sessionId;
        final int areaCode = MapZoneManager.computeAreaCode(currentLevel, deathX, deathY);
        executor.submit(() -> {
            try (Connection conn = db.getConnection()) {
                try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT_DEATH)) {
                    ps.setInt(1, sid);
                    if (areaCode >= 0) {
                        ps.setInt(2, areaCode);
                    } else {
                        ps.setNull(2, java.sql.Types.INTEGER);
                    }
                    ps.setDouble(3, deathX);
                    ps.setDouble(4, deathY);
                    ps.setString(5, deathCause);
                    ps.executeUpdate();
                    logger.debug("Telemetry: Death recorded at ({}, {}) cause={}", deathX, deathY, deathCause);
                }
            } catch (SQLException e) {
                logger.warn("Telemetry: Failed to record death: {}", e.getMessage());
            }
        });
    }

    // ====================================================================
    // PLAYER EVENTS
    // ====================================================================

    /**
     * Record a generic player event.
     * 
     * @param eventType Type of event (e.g., "FOOD_EATEN", "WEB_ESCAPE", "LEVEL_COMPLETE",
     *                  "DASH_USED", "TOY_PICKED_UP", "TOY_THROWN", "SPEED_BOOST",
     *                  "TRIPWIRE_HIT", "SPIDER_ALERT", "SAVE_GAME")
     * @param eventX    X coordinate where event occurred (nullable)
     * @param eventY    Y coordinate where event occurred (nullable)
     * @param eventValue Optional integer value (e.g., hunger level, food type ordinal)
     */
    public void recordEvent(String eventType, Double eventX, Double eventY, Integer eventValue) {
        if (!isOperational() || sessionId < 0) return;
        final int sid = sessionId;
        final int areaCode = (eventX != null && eventY != null)
                ? MapZoneManager.computeAreaCode(currentLevel, eventX, eventY)
                : -1;
        executor.submit(() -> {
            try (Connection conn = db.getConnection()) {
                try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT_EVENT)) {
                    ps.setInt(1, sid);
                    if (areaCode >= 0) {
                        ps.setInt(2, areaCode);
                    } else {
                        ps.setNull(2, java.sql.Types.INTEGER);
                    }
                    ps.setString(3, eventType);
                    if (eventX != null) {
                        ps.setDouble(4, eventX);
                    } else {
                        ps.setNull(4, java.sql.Types.DECIMAL);
                    }
                    if (eventY != null) {
                        ps.setDouble(5, eventY);
                    } else {
                        ps.setNull(5, java.sql.Types.DECIMAL);
                    }
                    if (eventValue != null) {
                        ps.setInt(6, eventValue);
                    } else {
                        ps.setNull(6, java.sql.Types.INTEGER);
                    }
                    ps.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));
                    ps.executeUpdate();
                }
                logger.trace("Telemetry: Event '{}' at ({}, {})", eventType, eventX, eventY);
            } catch (SQLException e) {
                logger.warn("Telemetry: Failed to record event '{}': {}", eventType, e.getMessage());
            }
        });
    }

    /**
     * Convenience: record an event with only type and position, no value.
     */
    public void recordEvent(String eventType, double eventX, double eventY) {
        recordEvent(eventType, eventX, eventY, null);
    }

    /**
     * Convenience: record an event with type only (no position or value).
     */
    public void recordEvent(String eventType) {
        recordEvent(eventType, null, null, null);
    }

    // ====================================================================
    // LIFECYCLE
    // ====================================================================

    /**
     * Get the current session ID (for external tracking).
     */
    public int getSessionId() {
        return sessionId;
    }

    /**
     * Get the current player ID (for external tracking).
     */
    public int getPlayerId() {
        return playerId;
    }

    /**
     * Shutdown the telemetry client gracefully.
     * Drains pending writes and closes the database pool.
     */
    public void shutdown() {
        if (executor != null) {
            // End any active session
            if (sessionId > 0) {
                endSession();
            }
            
            // Tell executor to stop accepting new tasks
            executor.shutdown();
            try {
                // Wait for previously submitted tasks (like endSession) to execute
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    logger.warn("Telemetry: Writer thread did not drain in time, forcing shutdown");
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                logger.warn("Telemetry: Shutdown interrupted");
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        // Close the database connection ONLY after the executor has finished its tasks
        if (db != null) {
            // If shutdown() is an instance method on DatabaseManager, use db.shutdown()
            // If it is a static method, keep it as DatabaseManager.shutdown()
            db.shutdown(); 
        }

        instance = null;
        logger.info("Telemetry client shutdown complete");
    }
}
