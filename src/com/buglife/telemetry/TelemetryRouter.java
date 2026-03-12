package com.buglife.telemetry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TelemetryRouter — Dual-Channel Dispatcher.
 * 
 * Forwards every telemetry call to both the SQL backend (TelemetryClient → MySQL)
 * and the Overseer HTTP backend (MongoTelemetryClient → MongoDB). Each backend
 * operates independently; if one is disabled or unavailable, the other continues
 * unaffected.
 * 
 * Game code should call TelemetryRouter instead of TelemetryClient directly.
 * The router is a singleton and delegates transparently.
 */
public class TelemetryRouter {
    private static final Logger logger = LoggerFactory.getLogger(TelemetryRouter.class);

    private static TelemetryRouter instance;

    private final TelemetryClient sqlClient;
    private final MongoTelemetryClient mongoClient;

    private TelemetryRouter() {
        this.sqlClient = TelemetryClient.getInstance();
        this.mongoClient = MongoTelemetryClient.getInstance();

        logger.info("TelemetryRouter initialized — SQL: {}, Overseer: {}",
                sqlClient.isOperational() ? "active" : "inactive",
                mongoClient.isOperational() ? "active" : "inactive");
    }

    public static synchronized TelemetryRouter getInstance() {
        if (instance == null) {
            instance = new TelemetryRouter();
        }
        return instance;
    }

    // ====================================================================
    // PLAYER MANAGEMENT
    // ====================================================================

    /**
     * Identify/register a player on both backends.
     */
    public void identifyPlayer(String username) {
        sqlClient.identifyPlayer(username);
        mongoClient.identifyPlayer(username);
    }

    // ====================================================================
    // SAVE FILE TRACKING
    // ====================================================================

    /**
     * Update a save file slot on both backends.
     */
    public void updateSaveFile(int slotNumber, double completionPct) {
        sqlClient.updateSaveFile(slotNumber, completionPct);
        mongoClient.updateSaveFile(slotNumber, completionPct);
    }

    // ====================================================================
    // SESSION MANAGEMENT
    // ====================================================================

    /**
     * Start a gameplay session on both backends.
     */
    public void startSession() {
        sqlClient.startSession();
        mongoClient.startSession();
    }

    /**
     * End the active session on both backends.
     */
    public void endSession() {
        sqlClient.endSession();
        mongoClient.endSession();
    }

    // ====================================================================
    // LEVEL / AREA TRACKING
    // ====================================================================

    /**
     * Notify both backends of the current level for area_code resolution.
     */
    public void setCurrentLevel(String levelName) {
        sqlClient.setCurrentLevel(levelName);
        mongoClient.setCurrentLevel(levelName);
    }

    // ====================================================================
    // DEATH EVENTS
    // ====================================================================

    /**
     * Record a death event on both backends.
     */
    public void recordDeath(double deathX, double deathY, String deathCause) {
        sqlClient.recordDeath(deathX, deathY, deathCause);
        mongoClient.recordDeath(deathX, deathY, deathCause);
    }

    // ====================================================================
    // PLAYER EVENTS
    // ====================================================================

    /**
     * Record a generic event on both backends.
     */
    public void recordEvent(String eventType, Double eventX, Double eventY, Integer eventValue) {
        sqlClient.recordEvent(eventType, eventX, eventY, eventValue);
        mongoClient.recordEvent(eventType, eventX, eventY, eventValue);
    }

    /** Convenience: event with position only. */
    public void recordEvent(String eventType, double eventX, double eventY) {
        sqlClient.recordEvent(eventType, eventX, eventY);
        mongoClient.recordEvent(eventType, eventX, eventY);
    }

    /** Convenience: event with type only. */
    public void recordEvent(String eventType) {
        sqlClient.recordEvent(eventType);
        mongoClient.recordEvent(eventType);
    }

    // ====================================================================
    // ACCESSORS
    // ====================================================================

    /** SQL backend operational? */
    public boolean isSqlOperational() {
        return sqlClient.isOperational();
    }

    /** Overseer (MongoDB) backend operational? */
    public boolean isMongoOperational() {
        return mongoClient.isOperational();
    }

    /** Either backend active? */
    public boolean isAnyOperational() {
        return sqlClient.isOperational() || mongoClient.isOperational();
    }

    /** SQL session ID (for debugging). */
    public int getSqlSessionId() {
        return sqlClient.getSessionId();
    }

    /** Overseer session ID (for debugging). */
    public int getMongoSessionId() {
        return mongoClient.getSessionId();
    }

    // ====================================================================
    // LIFECYCLE
    // ====================================================================

    /**
     * Shutdown both telemetry backends gracefully.
     */
    public void shutdown() {
        logger.info("TelemetryRouter shutting down both backends...");
        sqlClient.shutdown();
        mongoClient.shutdown();
        instance = null;
        logger.info("TelemetryRouter shutdown complete");
    }
}
