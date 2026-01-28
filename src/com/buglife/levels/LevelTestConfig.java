package com.buglife.levels;

import java.awt.Point;
import java.util.Arrays;
import java.util.List;

/**
 * ============================================================
 *                      TEST LEVEL
 * ============================================================
 * 
 * Development/testing level with ALL mechanics enabled.
 * Use this level to test new features!
 * 
 * ENABLED MECHANICS:
 *   ✓ Dash ability (SHIFT)
 *   ✓ Toy (pick up E, throw F)
 *   ✓ Tripwires
 *   ✓ Speed boost food (ENERGY_SEED)
 * 
 * Uses Level 1 map but with all features unlocked.
 */
public class LevelTestConfig implements LevelConfig {

    @Override
    public String getLevelName() {
        return "level_test";
    }
    
    // ============================================================
    //                     PLAYER SPAWN
    // ============================================================
    
    @Override
    public Point getPlayerSpawn() {
        // Same as Level 1
        return new Point(594, 2484);
    }
    
    // ============================================================
    //                      MECHANICS
    // ============================================================
    
    @Override
    public MechanicsConfig getMechanicsEnabled() {
        // TEST LEVEL: Everything enabled!
        return new MechanicsConfig()
            .enableDash()              // SHIFT to dash
            .enableToy()               // Pick up and throw toy
            .enableTripWires()         // Tripwire traps active
            .enableSpeedBoostFood();   // ENERGY_SEED spawns
    }
    
    // ============================================================
    //                     TOY SPAWN
    // ============================================================
    
    @Override
    public Point getToySpawn() {
        // Toy spawns near player start
        return new Point(574, 2256);
    }
    
    // ============================================================
    //                     TRIPWIRES
    // ============================================================
    //
    // Tripwires alert nearby spiders when player walks through.
    // Positions are in PIXELS.
    //
    // ============================================================
    
    @Override
    public List<Point> getTripWirePositions() {
        return Arrays.asList(
            new Point(1718, 1770),  // Mid-level trap
            new Point(800, 2400)   // Near starting area
        );
    }
    
    // ============================================================
    //                      SPIDERS
    // ============================================================
    
    @Override
    public List<SpiderPatrolData> getSpiderPatrols() {
        // Same as Level 1
        return Arrays.asList(
            SpiderPatrolData.rectangle(23, 23, 29, 25)
                .describe("Bottom-right patrol"),
            SpiderPatrolData.rectangle(2, 2, 8, 7)
                .describe("Top-left patrol"),
            SpiderPatrolData.vertical(26, 1, 10)
                .describe("Right wall vertical"),
            SpiderPatrolData.rectangle(7, 10, 15, 15)
                .describe("Middle patrol")
        );
    }
    
    // ============================================================
    //                       SNAILS
    // ============================================================
    
    @Override
    public List<SnailLocationData> getSnailLocations() {
        // Same as Level 1
        return Arrays.asList(
            SnailLocationData.at(534, 2464)
                .withDialogue("Hello little one...", "Be careful of the spiders!")
                .requiresInteraction(),
            SnailLocationData.at(938, 1754)
                .withDialogue("You shouldn't stay hungry", "Eat these berries.", "These give you energy")
                .requiresInteraction(),
            SnailLocationData.at(1116, 976)
                .withDialogue("There are dark shadows.", "You can hide from the spiders in it")
                .requiresInteraction(),
            SnailLocationData.at(2166, 136)
                .withDialogue("Stay safe", "Climb these ladders to the next floor.", "Farewell little one...!")
                .requiresInteraction()
        );
    }
    
    // ============================================================
    //                        FOOD
    // ============================================================
    //
    // Test level includes ENERGY_SEED for testing speed boost!
    //
    // ============================================================
    
    @Override
    public List<FoodSpawnData> getFoodSpawns() {
        return Arrays.asList(
            
            // Regular berries
            FoodSpawnData.berry(16, 27).describe("Near start"),
            FoodSpawnData.berry(34, 25).describe("Right side"),
            FoodSpawnData.berry(5, 5).describe("Top-left"),
            
            // Speed boost food (test these!)
            FoodSpawnData.energySeed(22, 10).describe("Strategic boost location 1"),
            FoodSpawnData.energySeed(15, 15).describe("Strategic boost location 2")
        );
    }
}
