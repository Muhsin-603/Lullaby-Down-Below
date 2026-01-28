package com.buglife.levels;

import java.awt.Point;
import java.util.List;

/**
 * Interface for level configuration.
 * 
 * Each level (Level1Config, Level2Config, etc.) implements this interface
 * to define all spawns, positions, and mechanics for that level.
 * 
 * HOW TO CREATE A NEW LEVEL:
 * 1. Create a new file: LevelXConfig.java
 * 2. Implement this interface
 * 3. Define your spawns and mechanics
 * 4. Register it in LevelConfigFactory
 * 
 * See Level1Config.java for a complete example.
 */
public interface LevelConfig {
    
    // ========== LEVEL IDENTITY ==========
    
    /** Get the level name (e.g., "level1", "level2") */
    String getLevelName();
    
    // ========== PLAYER SPAWN ==========
    
    /** 
     * Get player spawn position in PIXELS.
     * This is where the player starts when the level loads.
     */
    Point getPlayerSpawn();
    
    // ========== MECHANICS ==========
    
    /**
     * Get which mechanics are enabled for this level.
     * 
     * Example:
     *   return new MechanicsConfig()
     *       .enableDash()
     *       .enableToy();
     */
    MechanicsConfig getMechanicsEnabled();
    
    // ========== TOY SPAWN ==========
    
    /**
     * Get toy spawn position in PIXELS.
     * Return null if toy is disabled for this level.
     */
    Point getToySpawn();
    
    // ========== TRIPWIRES ==========
    
    /**
     * Get all tripwire positions in PIXELS.
     * Return empty list if no tripwires in this level.
     */
    List<Point> getTripWirePositions();
    
    // ========== SPIDERS ==========
    
    /**
     * Get all spider patrol paths for this level.
     * Each SpiderPatrolData defines one spider's waypoints.
     * 
     * Example:
     *   return Arrays.asList(
     *       SpiderPatrolData.rectangle(2, 2, 10, 8),
     *       SpiderPatrolData.horizontal(5, 15, 20)
     *   );
     */
    List<SpiderPatrolData> getSpiderPatrols();
    
    // ========== SNAILS ==========
    
    /**
     * Get all snail locations with dialogue.
     * The snail teleports between these locations.
     * 
     * Example:
     *   return Arrays.asList(
     *       SnailLocationData.at(534, 2464)
     *           .withDialogue("Hello!", "Be careful!")
     *           .requiresInteraction()
     *   );
     */
    List<SnailLocationData> getSnailLocations();
    
    // ========== FOOD ==========
    
    /**
     * Get all food spawn locations.
     * Note: ENERGY_SEED spawns are filtered out if speedBoostFood is disabled.
     * 
     * Example:
     *   return Arrays.asList(
     *       FoodSpawnData.berry(16, 27),
     *       FoodSpawnData.berry(34, 25),
     *       FoodSpawnData.energySeed(22, 10)  // Only if speedBoostFood enabled
     *   );
     */
    List<FoodSpawnData> getFoodSpawns();
}
