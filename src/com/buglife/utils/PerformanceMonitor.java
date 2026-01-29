package com.buglife.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.InputStream;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Performance monitoring utility for tracking FPS, memory usage, and game metrics.
 * Provides debug information overlay and performance logging.
 * Extended with comprehensive debug features: spider controls, visualization, timing, entity tracking.
 */
public class PerformanceMonitor {
    private static final Logger logger = LoggerFactory.getLogger(PerformanceMonitor.class);
    private static PerformanceMonitor instance;
    private static final String DEBUG_CONFIG_PATH = "src/main/resources/debug-settings.json";
    
    // private long lastFrameTime;
    private long frameCount;
    private double currentFPS;
    private double averageFPS;
    private long fpsUpdateTime;
    
    private long totalFrames;
    private long startTime;
    
    // Memory tracking
    private long usedMemory;
    private long maxMemory;
    private long totalMemory;
    
    // Performance thresholds
    private static final double TARGET_FPS = 60.0;
    private static final long FPS_UPDATE_INTERVAL = 1000; // milliseconds
    private static final long MEMORY_CHECK_INTERVAL = 5000; // milliseconds
    
    private long lastMemoryCheck;
    private boolean showDebugOverlay;
    
    // Spider debug controls
    private boolean spiderPatrolEnabled;
    private boolean spiderDetectionEnabled;
    private boolean showSpiderToggles;
    
    // Level selection menu
    private boolean showLevelMenu;
    private String[] availableLevels = {"level1", "level2", "level3", "level4", "level5"};
    private int selectedLevelIndex = 0;
    
    // Player coordinates
    private int playerX;
    private int playerY;
    
    // Visualization toggles
    private boolean showHitboxes;
    private boolean showTileGrid;
    private boolean showSpiderPaths;
    private boolean godMode;
    
    // Timing tracking
    private double updateTimeMs;
    private double renderTimeMs;
    private double[] frameTimes;
    private int frameTimeIndex;
    
    // Entity counts
    private int spiderCount;
    private int snailCount;
    private int foodCount;
    
    // Player state
    private String playerState;
    private int playerHunger;
    private double playerSpeed;
    private boolean playerWebbed;
    private boolean playerHasToy;
    
    // Level info
    private String currentLevel;
    
    private PerformanceMonitor() {
        this.startTime = System.nanoTime();
        // this.lastFrameTime = System.nanoTime();
        this.fpsUpdateTime = System.currentTimeMillis();
        this.lastMemoryCheck = System.currentTimeMillis();
        
        // Initialize frame times array
        this.frameTimes = new double[60];
        this.frameTimeIndex = 0;
        
        // Load debug settings from JSON
        loadDebugConfig();
    }
    
    public static PerformanceMonitor getInstance() {
        if (instance == null) {
            instance = new PerformanceMonitor();
        }
        return instance;
    }
    
    /**
     * Update performance metrics. Call once per frame.
     */
    public void update() {
        long currentTime = System.nanoTime();
        long currentMillis = System.currentTimeMillis();
        
        // Update frame timing
        frameCount++;
        totalFrames++;
        
        // Calculate FPS every second
        if (currentMillis - fpsUpdateTime >= FPS_UPDATE_INTERVAL) {
            double elapsedSeconds = (currentMillis - fpsUpdateTime) / 1000.0;
            currentFPS = frameCount / elapsedSeconds;
            averageFPS = totalFrames / ((currentTime - startTime) / 1_000_000_000.0);
            
            frameCount = 0;
            fpsUpdateTime = currentMillis;
            
            // Log warning if FPS drops below target
            if (currentFPS < TARGET_FPS * 0.9) {
                logger.warn("FPS drop detected: {:.1f} FPS (target: {})", currentFPS, TARGET_FPS);
            }
        }
        
        // Update memory stats periodically
        if (currentMillis - lastMemoryCheck >= MEMORY_CHECK_INTERVAL) {
            updateMemoryStats();
            lastMemoryCheck = currentMillis;
            
            // Log memory usage
            logger.debug("Memory: {} MB / {} MB (max: {} MB)", 
                    usedMemory / 1_048_576, 
                    totalMemory / 1_048_576, 
                    maxMemory / 1_048_576);
        }
        
        // lastFrameTime = currentTime;
    }
    
    /**
     * Set timing information for current frame
     */
    public void setFrameTiming(double updateMs, double renderMs) {
        this.updateTimeMs = updateMs;
        this.renderTimeMs = renderMs;
        
        // Store frame time for graph
        double totalFrameTime = updateMs + renderMs;
        frameTimes[frameTimeIndex] = totalFrameTime;
        frameTimeIndex = (frameTimeIndex + 1) % frameTimes.length;
    }
    
    /**
     * Get frame times for graph rendering
     */
    public double[] getFrameTimes() {
        return frameTimes;
    }
    
    /**
     * Get update time in milliseconds
     */
    public double getUpdateTimeMs() {
        return updateTimeMs;
    }
    
    /**
     * Get render time in milliseconds
     */
    public double getRenderTimeMs() {
        return renderTimeMs;
    }
    
    /**
     * Update memory statistics
     */
    private void updateMemoryStats() {
        Runtime runtime = Runtime.getRuntime();
        totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        maxMemory = runtime.maxMemory();
        usedMemory = totalMemory - freeMemory;
        
        // Warn if memory usage is high
        double memoryUsagePercent = (double) usedMemory / maxMemory * 100;
        if (memoryUsagePercent > 80) {
            logger.warn("High memory usage: {:.1f}%", memoryUsagePercent);
        }
    }
    
    /**
     * Get current FPS
     */
    public double getCurrentFPS() {
        return currentFPS;
    }
    
    /**
     * Get average FPS since start
     */
    public double getAverageFPS() {
        return averageFPS;
    }
    
    /**
     * Get total frames rendered
     */
    public long getTotalFrames() {
        return totalFrames;
    }
    
    /**
     * Get used memory in bytes
     */
    public long getUsedMemory() {
        return usedMemory;
    }
    
    /**
     * Get used memory in megabytes
     */
    public double getUsedMemoryMB() {
        return usedMemory / 1_048_576.0;
    }
    
    /**
     * Get total available memory in megabytes
     */
    public double getTotalMemoryMB() {
        return totalMemory / 1_048_576.0;
    }
    
    /**
     * Get maximum memory in megabytes
     */
    public double getMaxMemoryMB() {
        return maxMemory / 1_048_576.0;
    }
    
    /**
     * Get memory usage percentage
     */
    public double getMemoryUsagePercent() {
        return (double) usedMemory / maxMemory * 100.0;
    }
    
    // ========== SPIDER DEBUG CONTROLS ==========
    
    /**
     * Toggle spider debug menu visibility
     */
    public void toggleSpiderTogglesMenu() {
        showSpiderToggles = !showSpiderToggles;
        logger.info("Spider debug menu: {}", showSpiderToggles ? "ON" : "OFF");
        saveDebugConfig();
    }
    
    /**
     * Toggle spider patrol movement
     */
    public void toggleSpiderPatrol() {
        spiderPatrolEnabled = !spiderPatrolEnabled;
        logger.info("Spider patrol: {}", spiderPatrolEnabled ? "ENABLED" : "DISABLED");
        saveDebugConfig();
    }
    
    /**
     * Toggle spider detection/chasing
     */
    public void toggleSpiderDetection() {
        spiderDetectionEnabled = !spiderDetectionEnabled;
        logger.info("Spider detection: {}", spiderDetectionEnabled ? "ENABLED" : "DISABLED");
        saveDebugConfig();
    }
    
    /**
     * Check if spider patrol is enabled
     */
    public boolean isSpiderPatrolEnabled() {
        return spiderPatrolEnabled;
    }
    
    /**
     * Check if spider detection is enabled
     */
    public boolean isSpiderDetectionEnabled() {
        return spiderDetectionEnabled;
    }
    
    /**
     * Check if spider toggles menu should be shown
     */
    public boolean isSpiderTogglesVisible() {
        return showSpiderToggles;
    }
    
    // ========== LEVEL SELECTION MENU ==========
    
    /**
     * Toggle level selection menu visibility
     */
    public void toggleLevelMenu() {
        showLevelMenu = !showLevelMenu;
        logger.info("Level selection menu: {}", showLevelMenu ? "ON" : "OFF");
        saveDebugConfig();
    }
    
    /**
     * Check if level menu should be shown
     */
    public boolean isLevelMenuVisible() {
        return showLevelMenu;
    }
    
    /**
     * Get array of available levels
     */
    public String[] getAvailableLevels() {
        return availableLevels;
    }
    
    /**
     * Get selected level index
     */
    public int getSelectedLevelIndex() {
        return selectedLevelIndex;
    }
    
    /**
     * Set selected level index
     */
    public void setSelectedLevelIndex(int index) {
        if (index >= 0 && index < availableLevels.length) {
            this.selectedLevelIndex = index;
        }
    }
    
    /**
     * Get currently selected level name
     */
    public String getSelectedLevel() {
        return availableLevels[selectedLevelIndex];
    }
    
    /**
     * Move level selection up
     */
    public void levelSelectionUp() {
        selectedLevelIndex = (selectedLevelIndex - 1 + availableLevels.length) % availableLevels.length;
    }
    
    /**
     * Move level selection down
     */
    public void levelSelectionDown() {
        selectedLevelIndex = (selectedLevelIndex + 1) % availableLevels.length;
    }
    
    // ========== PLAYER COORDINATES ==========
    
    /**
     * Set player coordinates
     */
    public void setPlayerCoordinates(int x, int y) {
        this.playerX = x;
        this.playerY = y;
    }
    
    /**
     * Get player X coordinate
     */
    public int getPlayerX() {
        return playerX;
    }
    
    /**
     * Get player Y coordinate
     */
    public int getPlayerY() {
        return playerY;
    }
    
    // ========== VISUALIZATION TOGGLES ==========
    
    /**
     * Toggle hitbox visualization
     */
    public void toggleHitboxes() {
        showHitboxes = !showHitboxes;
        logger.info("Hitbox visualization: {}", showHitboxes ? "ON" : "OFF");
        saveDebugConfig();
    }
    
    /**
     * Toggle tile grid overlay
     */
    public void toggleTileGrid() {
        showTileGrid = !showTileGrid;
        logger.info("Tile grid: {}", showTileGrid ? "ON" : "OFF");
        saveDebugConfig();
    }
    
    /**
     * Toggle spider path visualization
     */
    public void toggleSpiderPaths() {
        showSpiderPaths = !showSpiderPaths;
        logger.info("Spider paths: {}", showSpiderPaths ? "ON" : "OFF");
        saveDebugConfig();
    }
    
    /**
     * Toggle god mode
     */
    public void toggleGodMode() {
        godMode = !godMode;
        logger.info("God mode: {}", godMode ? "ON" : "OFF");
        saveDebugConfig();
    }
    
    /**
     * Check if hitboxes should be shown
     */
    public boolean isShowHitboxes() {
        return showHitboxes;
    }
    
    /**
     * Check if tile grid should be shown
     */
    public boolean isShowTileGrid() {
        return showTileGrid;
    }
    
    /**
     * Check if spider paths should be shown
     */
    public boolean isShowSpiderPaths() {
        return showSpiderPaths;
    }
    
    /**
     * Check if god mode is enabled
     */
    public boolean isGodModeEnabled() {
        return godMode;
    }
    
    // ========== ENTITY TRACKING ==========
    
    /**
     * Set entity counts
     */
    public void setEntityCounts(int spiders, int snails, int foods) {
        this.spiderCount = spiders;
        this.snailCount = snails;
        this.foodCount = foods;
    }
    
    /**
     * Get spider count
     */
    public int getSpiderCount() {
        return spiderCount;
    }
    
    /**
     * Get snail count
     */
    public int getSnailCount() {
        return snailCount;
    }
    
    /**
     * Get food count
     */
    public int getFoodCount() {
        return foodCount;
    }
    
    // ========== PLAYER STATE ==========
    
    /**
     * Set player state information
     */
    public void setPlayerState(String state, int hunger, double speed, boolean webbed, boolean hasToy) {
        this.playerState = state;
        this.playerHunger = hunger;
        this.playerSpeed = speed;
        this.playerWebbed = webbed;
        this.playerHasToy = hasToy;
    }
    
    /**
     * Get player state string
     */
    public String getPlayerState() {
        return playerState != null ? playerState : "UNKNOWN";
    }
    
    /**
     * Get player hunger
     */
    public int getPlayerHunger() {
        return playerHunger;
    }
    
    /**
     * Get player speed
     */
    public double getPlayerSpeed() {
        return playerSpeed;
    }
    
    /**
     * Is player webbed
     */
    public boolean isPlayerWebbed() {
        return playerWebbed;
    }
    
    /**
     * Does player have toy
     */
    public boolean isPlayerHasToy() {
        return playerHasToy;
    }
    
    // ========== LEVEL INFO ==========
    
    /**
     * Set current level name
     */
    public void setCurrentLevel(String level) {
        this.currentLevel = level;
    }
    
    /**
     * Get current level name
     */
    public String getCurrentLevel() {
        return currentLevel != null ? currentLevel : "Unknown";
    }
    
    // ========== CONFIG PERSISTENCE ==========
    
    /**
     * Load debug configuration from JSON file
     */
    private void loadDebugConfig() {
        try {
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("debug-settings.json");
            if (inputStream != null) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(inputStream);
                
                spiderPatrolEnabled = root.path("spiderPatrolEnabled").asBoolean(true);
                spiderDetectionEnabled = root.path("spiderDetectionEnabled").asBoolean(true);
                showHitboxes = root.path("showHitboxes").asBoolean(false);
                showTileGrid = root.path("showTileGrid").asBoolean(false);
                showSpiderPaths = root.path("showSpiderPaths").asBoolean(false);
                godMode = root.path("godMode").asBoolean(false);
                showDebugOverlay = root.path("showDebugOverlay").asBoolean(false);
                showLevelMenu = root.path("showLevelMenu").asBoolean(false);
                
                logger.info("Debug configuration loaded successfully");
            } else {
                // Use defaults if file not found
                setDefaultDebugSettings();
                logger.warn("debug-settings.json not found, using defaults");
            }
        } catch (Exception e) {
            logger.error("Failed to load debug configuration, using defaults", e);
            setDefaultDebugSettings();
        }
    }
    
    /**
     * Save debug configuration to JSON file
     */
    private void saveDebugConfig() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            
            // Create JSON structure
            String json = String.format(
                "{\n" +
                "  \"spiderPatrolEnabled\": %s,\n" +
                "  \"spiderDetectionEnabled\": %s,\n" +
                "  \"showHitboxes\": %s,\n" +
                "  \"showTileGrid\": %s,\n" +
                "  \"showSpiderPaths\": %s,\n" +
                "  \"godMode\": %s,\n" +
                "  \"showDebugOverlay\": %s,\n" +
                "  \"showLevelMenu\": %s\n" +
                "}",
                spiderPatrolEnabled,
                spiderDetectionEnabled,
                showHitboxes,
                showTileGrid,
                showSpiderPaths,
                godMode,
                showDebugOverlay,
                showLevelMenu
            );
            
            // Write to file
            try (FileWriter writer = new FileWriter(DEBUG_CONFIG_PATH)) {
                writer.write(json);
            }
            
            logger.debug("Debug configuration saved");
        } catch (Exception e) {
            logger.error("Failed to save debug configuration", e);
        }
    }
    
    /**
     * Set default debug settings
     */
    private void setDefaultDebugSettings() {
        spiderPatrolEnabled = true;
        spiderDetectionEnabled = true;
        showHitboxes = false;
        showTileGrid = false;
        showSpiderPaths = false;
        godMode = false;
        showDebugOverlay = false;
        showSpiderToggles = false;
        showLevelMenu = false;
    }
    
    /**
     * Toggle debug overlay
     */
    public void toggleDebugOverlay() {
        showDebugOverlay = !showDebugOverlay;
        logger.info("Debug overlay: {}", showDebugOverlay ? "ON" : "OFF");
        saveDebugConfig();
    }
    
    /**
     * Check if debug overlay should be shown
     */
    public boolean isDebugOverlayEnabled() {
        return showDebugOverlay;
    }
    
    /**
     * Set debug overlay state
     */
    public void setDebugOverlay(boolean enabled) {
        this.showDebugOverlay = enabled;
    }
    
    /**
     * Get formatted performance statistics string
     */
    public String getStats() {
        return String.format(
            "FPS: %.1f (avg: %.1f) | Frames: %d | Memory: %.1f/%.1f MB (%.1f%%)",
            currentFPS, averageFPS, totalFrames,
            getUsedMemoryMB(), getMaxMemoryMB(), getMemoryUsagePercent()
        );
    }
    
    /**
     * Force garbage collection (use sparingly)
     */
    public void forceGC() {
        logger.info("Forcing garbage collection...");
        long beforeGC = usedMemory;
        System.gc();
        updateMemoryStats();
        long freedMemory = beforeGC - usedMemory;
        logger.info("GC complete. Freed {} MB", freedMemory / 1_048_576);
    }
    
    /**
     * Reset statistics
     */
    public void reset() {
        frameCount = 0;
        totalFrames = 0;
        currentFPS = 0;
        averageFPS = 0;
        startTime = System.nanoTime();
        fpsUpdateTime = System.currentTimeMillis();
        logger.info("Performance monitor reset");
    }
    
    /**
     * Log current performance summary
     */
    public void logSummary() {
        logger.info("=== Performance Summary ===");
        logger.info("Current FPS: {:.1f}", currentFPS);
        logger.info("Average FPS: {:.1f}", averageFPS);
        logger.info("Total Frames: {}", totalFrames);
        logger.info("Memory Usage: {:.1f} MB / {:.1f} MB ({:.1f}%)", 
                getUsedMemoryMB(), getMaxMemoryMB(), getMemoryUsagePercent());
        logger.info("========================");
    }
}
