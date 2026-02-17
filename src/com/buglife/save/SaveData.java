package com.buglife.save;

import java.util.ArrayList;
import java.util.List;

/**
 * SaveData — The "digital photograph" of a player's exact state of misery.
 * 
 * This is the JSON-serializable snapshot that gets frozen to disk and
 * uploaded to the Overseer. Every spider web walked into, every berry
 * eaten, every ounce of hunger drained — all captured here.
 * 
 * Format: JSON via Jackson ObjectMapper.
 */
public class SaveData {

    // === THE WHERE ===
    private String levelId;          // e.g. "level1", "level3"
    private int levelIndex;          // Index in the level order array
    private double playerX;          // Exact X coordinate in world pixels
    private double playerY;          // Exact Y coordinate in world pixels
    private String facingDirection;  // "UP", "DOWN", "LEFT", "RIGHT"

    // === THE BODY ===
    private int hunger;              // Current hunger level (0–100)
    private boolean isCrying;        // Was the player starving when they saved?
    private int speedBoostTimer;     // Remaining speed boost frames

    // === THE POCKETS ===
    private boolean hasToy;          // Is the player carrying the toy?
    private double toyX;             // Toy's world X (if not carried)
    private double toyY;             // Toy's world Y (if not carried)

    // === THE WORLD STATE (cruel but fair) ===
    private List<FoodState> remainingFoods;  // Which foods are still alive on the map

    // === META ===
    private String playerName;       // The arcade identity (e.g. "SHIBILI")
    private long saveTimestamp;      // When this photograph was taken (epoch millis)
    private String saveVersion;      // Schema version for future compatibility
    private long totalPlaytimeSeconds; // Total time spent in the game (all sessions)

    public SaveData() {
        this.remainingFoods = new ArrayList<>();
        this.saveVersion = "1.0";
    }

    // ─── Nested class for food state ────────────────────────────
    public static class FoodState {
        private int x;
        private int y;
        private String type; // "BERRY" or "ENERGY_SEED"

        public FoodState() {}

        public FoodState(int x, int y, String type) {
            this.x = x;
            this.y = y;
            this.type = type;
        }

        public int getX() { return x; }
        public void setX(int x) { this.x = x; }
        public int getY() { return y; }
        public void setY(int y) { this.y = y; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
    }

    // ─── Getters & Setters ──────────────────────────────────────

    public String getLevelId() { return levelId; }
    public void setLevelId(String levelId) { this.levelId = levelId; }

    public int getLevelIndex() { return levelIndex; }
    public void setLevelIndex(int levelIndex) { this.levelIndex = levelIndex; }

    public double getPlayerX() { return playerX; }
    public void setPlayerX(double playerX) { this.playerX = playerX; }

    public double getPlayerY() { return playerY; }
    public void setPlayerY(double playerY) { this.playerY = playerY; }

    public String getFacingDirection() { return facingDirection; }
    public void setFacingDirection(String facingDirection) { this.facingDirection = facingDirection; }

    public int getHunger() { return hunger; }
    public void setHunger(int hunger) { this.hunger = hunger; }

    public boolean isCrying() { return isCrying; }
    public void setCrying(boolean crying) { isCrying = crying; }

    public int getSpeedBoostTimer() { return speedBoostTimer; }
    public void setSpeedBoostTimer(int speedBoostTimer) { this.speedBoostTimer = speedBoostTimer; }

    public boolean isHasToy() { return hasToy; }
    public void setHasToy(boolean hasToy) { this.hasToy = hasToy; }

    public double getToyX() { return toyX; }
    public void setToyX(double toyX) { this.toyX = toyX; }

    public double getToyY() { return toyY; }
    public void setToyY(double toyY) { this.toyY = toyY; }

    public List<FoodState> getRemainingFoods() { return remainingFoods; }
    public void setRemainingFoods(List<FoodState> remainingFoods) { this.remainingFoods = remainingFoods; }

    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }

    public long getSaveTimestamp() { return saveTimestamp; }
    public void setSaveTimestamp(long saveTimestamp) { this.saveTimestamp = saveTimestamp; }

    public String getSaveVersion() { return saveVersion; }
    public void setSaveVersion(String saveVersion) { this.saveVersion = saveVersion; }

    public long getTotalPlaytimeSeconds() { return totalPlaytimeSeconds; }
    public void setTotalPlaytimeSeconds(long totalPlaytimeSeconds) { this.totalPlaytimeSeconds = totalPlaytimeSeconds; }

    @Override
    public String toString() {
        return String.format("SaveData{player=%s, level=%s, pos=(%.0f,%.0f), hunger=%d, time=%d, playtime=%d}",
                playerName, levelId, playerX, playerY, hunger, saveTimestamp, totalPlaytimeSeconds);
    }
}
