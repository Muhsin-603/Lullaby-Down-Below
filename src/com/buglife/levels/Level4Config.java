package com.buglife.levels;

import java.awt.Point;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * ============================================================
 *                        LEVEL 4
 * ============================================================
 * 
 * TODO: Design this level!
 * 
 * Currently a placeholder with minimal content.
 * Copy the structure from Level1Config or Level2Config and customize.
 */
public class Level4Config implements LevelConfig {

    @Override
    public String getLevelName() {
        return "level4";
    }
    
    // ============================================================
    //                     PLAYER SPAWN
    // ============================================================
    
    @Override
    public Point getPlayerSpawn() {
        // TODO: Set spawn for Level 4 map
        return new Point(100, 100);
    }
    
    // ============================================================
    //                      MECHANICS
    // ============================================================
    
    @Override
    public MechanicsConfig getMechanicsEnabled() {
        // TODO: Decide which mechanics to enable
        return new MechanicsConfig();
    }
    
    // ============================================================
    //                     TOY SPAWN
    // ============================================================
    
    @Override
    public Point getToySpawn() {
        return null; // No toy
    }
    
    // ============================================================
    //                     TRIPWIRES
    // ============================================================
    
    @Override
    public List<Point> getTripWirePositions() {
        return Collections.emptyList();
    }
    
    // ============================================================
    //                      SPIDERS
    // ============================================================
    
    @Override
    public List<SpiderPatrolData> getSpiderPatrols() {
        // TODO: Add spider patrols for Level 4
        return Arrays.asList(
            // Example:
            // SpiderPatrolData.rectangle(5, 5, 15, 15)
            //     .describe("Main chamber patrol")
        );
    }
    
    // ============================================================
    //                       SNAILS
    // ============================================================
    
    @Override
    public List<SnailLocationData> getSnailLocations() {
        // TODO: Add snail locations for Level 4
        return Arrays.asList(
            // Example:
            // SnailLocationData.at(100, 100)
            //     .withDialogue("Welcome to Level 4!")
            //     .requiresInteraction()
        );
    }
    
    // ============================================================
    //                        FOOD
    // ============================================================
    
    @Override
    public List<FoodSpawnData> getFoodSpawns() {
        // TODO: Add food spawns for Level 4
        return Arrays.asList(
            // Example:
            // FoodSpawnData.berry(10, 10)
        );
    }
}
