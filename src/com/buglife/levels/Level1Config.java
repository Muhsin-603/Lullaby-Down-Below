package com.buglife.levels;

import java.awt.Point;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * ============================================================
 *                        LEVEL 1
 * ============================================================
 * 
 * The tutorial level - clean and simple.
 * 
 * DISABLED MECHANICS:
 *   - No dash ability
 *   - No toy
 *   - No tripwires
 *   - No speed boost food (ENERGY_SEED)
 * 
 * This level focuses on teaching basic movement and spider avoidance.
 */
public class Level1Config implements LevelConfig {

    @Override
    public String getLevelName() {
        return "level1";
    }
    
    // ============================================================
    //                     PLAYER SPAWN
    // ============================================================
    
    @Override
    public Point getPlayerSpawn() {
        // Starting position at bottom of the map
        return new Point(594, 2484);
    }
    
    // ============================================================
    //                      MECHANICS
    // ============================================================
    
    @Override
    public MechanicsConfig getMechanicsEnabled() {
        // Level 1 is clean - no advanced mechanics
        return new MechanicsConfig();
        // Nothing enabled = basic gameplay only
    }
    
    // ============================================================
    //                     TOY SPAWN
    // ============================================================
    
    @Override
    public Point getToySpawn() {
        // No toy in Level 1
        return null;
    }
    
    // ============================================================
    //                     TRIPWIRES
    // ============================================================
    
    @Override
    public List<Point> getTripWirePositions() {
        // No tripwires in Level 1
        return Collections.emptyList();
    }
    
    // ============================================================
    //                      SPIDERS
    // ============================================================
    // 
    // Spider patrol paths use TILE coordinates.
    // Each spider patrols between the given waypoints.
    //
    // TIP: Use the helper methods:
    //   - SpiderPatrolData.rectangle(left, top, right, bottom)
    //   - SpiderPatrolData.horizontal(startX, endX, y)
    //   - SpiderPatrolData.vertical(x, startY, endY)
    //   - SpiderPatrolData.custom().addPoint(x,y).addPoint(x,y).build()
    //
    // ============================================================
    
    @Override
    public List<SpiderPatrolData> getSpiderPatrols() {
        return Arrays.asList(
            
            // Spider 1: Bottom-right area patrol
            SpiderPatrolData.rectangle(23, 23, 29, 25)
                .describe("Guards the bottom-right chamber"),
            
            // Spider 2: Top-left corner patrol
            SpiderPatrolData.rectangle(2, 2, 8, 7)
                .describe("Patrols top-left spawn area"),
            
            // Spider 3: Vertical patrol on right side
            SpiderPatrolData.vertical(26, 1, 10)
                .describe("Vertical guard on right wall"),
            
            // Spider 4: Middle section patrol
            SpiderPatrolData.rectangle(7, 10, 15, 15)
                .describe("Middle chamber patrol")
        );
    }
    
    // ============================================================
    //                       SNAILS
    // ============================================================
    //
    // Snail locations use PIXEL coordinates.
    // The snail teleports to the next location when off-screen
    // (and after player interaction if required).
    //
    // TIP: Use the helper methods:
    //   - SnailLocationData.at(pixelX, pixelY)
    //   - SnailLocationData.atTile(tileX, tileY)
    //
    // ============================================================
    
    @Override
    public List<SnailLocationData> getSnailLocations() {
        return Arrays.asList(
            
            // Location 1: Starting area - greeting
            SnailLocationData.at(534, 2464)
                .withDialogue(
                    "Hello little one...",
                    "Be careful of the spiders!"
                )
                .requiresInteraction()
                .describe("Starting area guide"),
            
            // Location 2: Mid-level - food advice
            SnailLocationData.at(938, 1754)
                .withDialogue(
                    "You shouldn't stay hungry",
                    "Eat these berries.",
                    "These give you energy"
                )
                .requiresInteraction()
                .describe("Berry eating tutorial"),
            
            // Location 3: Shadow area hint
            SnailLocationData.at(1116, 976)
                .withDialogue(
                    "There are dark shadows.",
                    "You can hide from the spiders in it"
                )
                .requiresInteraction()
                .describe("Shadow hiding tutorial"),
            
            // Location 4: Near level exit
            SnailLocationData.at(2166, 136)
                .withDialogue(
                    "Stay safe",
                    "Climb these ladders to the next floor.",
                    "Farewell little one...!"
                )
                .requiresInteraction()
                .describe("Exit area farewell")
        );
    }
    
    // ============================================================
    //                        FOOD
    // ============================================================
    //
    // Food spawns use TILE coordinates.
    // 
    // Types:
    //   - FoodSpawnData.berry(x, y)      = Yellow, +25 hunger
    //   - FoodSpawnData.energySeed(x, y) = Green, +15 hunger + speed boost
    //                                      (only if speedBoostFood enabled!)
    //
    // ============================================================
    
    @Override
    public List<FoodSpawnData> getFoodSpawns() {
        return Arrays.asList(
            
            // Berries scattered around the level
            FoodSpawnData.berry(16, 27).describe("Near starting area"),
            FoodSpawnData.berry(34, 25).describe("Right side chamber"),
            FoodSpawnData.berry(5, 5).describe("Top-left corner")
            
            // No ENERGY_SEED in Level 1 (speedBoostFood disabled)
        );
    }
}
