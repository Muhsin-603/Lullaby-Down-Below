package com.buglife.levels;

import java.awt.Point;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * ============================================================
 *                        LEVEL 2
 * ============================================================
 * 
 * Second floor - increased difficulty.
 * 
 * DISABLED MECHANICS:
 *   - No dash ability
 *   - No toy
 *   - No tripwires
 *   - No speed boost food (ENERGY_SEED)
 * 
 * More spiders, more complex layout, more food needed.
 */
public class Level2Config implements LevelConfig {

    @Override
    public String getLevelName() {
        return "level2";
    }
    
    // ============================================================
    //                     PLAYER SPAWN
    // ============================================================
    
    @Override
    public Point getPlayerSpawn() {
        // TODO: Adjust this to Level 2 map's starting position
        // Currently using a placeholder near bottom of map
        return new Point(184, 2300);
    }
    
    // ============================================================
    //                      MECHANICS
    // ============================================================
    
    @Override
    public MechanicsConfig getMechanicsEnabled() {
        // Level 2 is also clean - no advanced mechanics yet
        return new MechanicsConfig();
        // Nothing enabled = basic gameplay only
    }
    
    // ============================================================
    //                     TOY SPAWN
    // ============================================================
    
    @Override
    public Point getToySpawn() {
        // No toy in Level 2
        return null;
    }
    
    // ============================================================
    //                     TRIPWIRES
    // ============================================================
    
    @Override
    public List<Point> getTripWirePositions() {
        // No tripwires in Level 2
        return Collections.emptyList();
    }
    
    // ============================================================
    //                      SPIDERS
    // ============================================================
    //
    // Level 2 map is 25 columns (0-24) x 76 rows (0-75)
    // Make sure patrol coordinates stay within bounds!
    //
    // ============================================================
    
    @Override
    public List<SpiderPatrolData> getSpiderPatrols() {
        return Arrays.asList(
            
            // Spider 1: Upper area patrol
            SpiderPatrolData.rectangle(5, 8, 10, 15)
                .describe("Upper chamber guard"),
            
            // Spider 2: Middle-right patrol
            SpiderPatrolData.rectangle(14, 20, 22, 35)
                .describe("Middle section large patrol"),
            
            // Spider 3: Left side patrol
            SpiderPatrolData.rectangle(2, 28, 12, 40)
                .describe("Left chamber patrol"),
            
            // Spider 4: Bottom area patrol
            SpiderPatrolData.rectangle(18, 50, 23, 63)
                .describe("Bottom chamber guard")
        );
    }
    
    // ============================================================
    //                       SNAILS
    // ============================================================
    
    @Override
    public List<SnailLocationData> getSnailLocations() {
        return Arrays.asList(
            
            // Location 1: Starting area
            SnailLocationData.at(184, 1856)
                .withDialogue(
                    "Welcome to the second floor...",
                    "The spiders here are more aggressive!"
                )
                .requiresInteraction()
                .describe("Level 2 intro"),
            
            // Location 2: Mid section
            SnailLocationData.at(684, 1144)
                .withDialogue(
                    "I sense danger ahead...",
                    "Stay alert and move carefully."
                )
                .requiresInteraction()
                .describe("Mid-level warning"),
            
            // Location 3: Shadow area
            SnailLocationData.at(1304, 704)
                .withDialogue(
                    "The shadows grow deeper here.",
                    "Use them wisely..."
                )
                .requiresInteraction()
                .describe("Shadow area tip"),
            
            // Location 4: Near exit
            SnailLocationData.at(1876, 280)
                .withDialogue(
                    "You're almost there...",
                    "The next floor awaits...",
                    "Be brave, little one!"
                )
                .requiresInteraction()
                .describe("Exit area encouragement")
        );
    }
    
    // ============================================================
    //                        FOOD
    // ============================================================
    //
    // More food in Level 2 to compensate for longer level
    //
    // ============================================================
    
    @Override
    public List<FoodSpawnData> getFoodSpawns() {
        return Arrays.asList(
            
            // Bottom chambers
            FoodSpawnData.berry(4, 52).describe("Bottom-left chamber"),
            FoodSpawnData.berry(10, 54).describe("Bottom-center"),
            
            // Middle section
            FoodSpawnData.berry(8, 35).describe("Middle-left"),
            FoodSpawnData.berry(4, 28).describe("Left passage"),
            
            // Upper chambers
            FoodSpawnData.berry(8, 12).describe("Upper-left"),
            FoodSpawnData.berry(18, 15).describe("Upper-center")
            
            // No ENERGY_SEED in Level 2 (speedBoostFood disabled)
        );
    }
}
