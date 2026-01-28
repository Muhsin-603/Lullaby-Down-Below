package com.buglife.levels;

import java.awt.Point;
import com.buglife.entities.Food;

/**
 * Defines a food spawn location and type.
 * 
 * USAGE: Create food spawns like this:
 * 
 *   // Berry at tile coordinates (restores hunger)
 *   FoodSpawnData.berry(16, 27)
 *   
 *   // Energy seed at tile coordinates (restores hunger + speed boost)
 *   FoodSpawnData.energySeed(22, 10)
 *   
 *   // Custom food type at tile
 *   FoodSpawnData.atTile(5, 5, Food.FoodType.BERRY)
 */
public class FoodSpawnData {
    
    private final Point tilePosition;
    private final Food.FoodType type;
    private String description; // Optional: for documentation
    
    private FoodSpawnData(int tileX, int tileY, Food.FoodType type) {
        this.tilePosition = new Point(tileX, tileY);
        this.type = type;
    }
    
    // ========== EASY FOOD BUILDERS ==========
    
    /**
     * Create a BERRY food spawn (yellow, +25 hunger).
     * @param tileX Tile X coordinate
     * @param tileY Tile Y coordinate
     */
    public static FoodSpawnData berry(int tileX, int tileY) {
        return new FoodSpawnData(tileX, tileY, Food.FoodType.BERRY);
    }
    
    /**
     * Create an ENERGY_SEED food spawn (green, +15 hunger + speed boost).
     * Note: Only spawns if level has speedBoostFood enabled!
     * @param tileX Tile X coordinate
     * @param tileY Tile Y coordinate
     */
    public static FoodSpawnData energySeed(int tileX, int tileY) {
        return new FoodSpawnData(tileX, tileY, Food.FoodType.ENERGY_SEED);
    }
    
    /**
     * Create food at tile with custom type.
     * @param tileX Tile X coordinate
     * @param tileY Tile Y coordinate
     * @param type Food type
     */
    public static FoodSpawnData atTile(int tileX, int tileY, Food.FoodType type) {
        return new FoodSpawnData(tileX, tileY, type);
    }
    
    // ========== DESCRIPTION (Optional) ==========
    
    /** Add a description for documentation purposes */
    public FoodSpawnData describe(String description) {
        this.description = description;
        return this;
    }
    
    public String getDescription() {
        return description;
    }
    
    // ========== GETTERS ==========
    
    public Point getTilePosition() {
        return tilePosition;
    }
    
    public Food.FoodType getType() {
        return type;
    }
    
    /** Check if this is a speed boost food type */
    public boolean isSpeedBoostFood() {
        return type == Food.FoodType.ENERGY_SEED;
    }
}
