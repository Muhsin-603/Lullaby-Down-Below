package com.buglife.levels;

import java.awt.Point;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * ============================================================
 *                        LEVEL 3
 * ============================================================
 * 
 * Third floor - introduces DASH ability and SPEED BOOST FOOD.
 * 
 * ENABLED MECHANICS:
 *   - Dash ability (SHIFT key) - Quick burst of speed
 *   - Speed boost food (ENERGY_SEED) - Gives temporary speed
 * 
 * DISABLED MECHANICS:
 *   - No toy
 *   - No tripwires
 * 
 * This level teaches players to use dash and speed boost food
 * to navigate through spider-infested areas.
 */
public class Level3Config implements LevelConfig {

    @Override
    public String getLevelName() {
        return "level3";
    }
    
    // ============================================================
    //                     PLAYER SPAWN
    // ============================================================
    
    @Override
    public Point getPlayerSpawn() {
        // Starting position - bottom area of the map
        return new Point(784, 2624);
    }
    
    // ============================================================
    //                      MECHANICS
    // ============================================================
    
    @Override
    public MechanicsConfig getMechanicsEnabled() {
        // Level 3 introduces dash and speed boost food
        return new MechanicsConfig()
            .enableDash()
            .enableSpeedBoostFood();
    }
    
    // ============================================================
    //                     TOY SPAWN
    // ============================================================
    
    @Override
    public Point getToySpawn() {
        // No toy in Level 3
        return null;
    }
    
    // ============================================================
    //                     TRIPWIRES
    // ============================================================
    
    @Override
    public List<Point> getTripWirePositions() {
        // No tripwires in Level 3
        return Collections.emptyList();
    }
    
    // ============================================================
    //                      SPIDERS
    // ============================================================
    //
    // Level 3 map is 50 columns (0-49) x 88 rows (0-87)
    // Spiders patrol key corridors and chokepoints
    //
    // ============================================================
    
    @Override
    public List<SpiderPatrolData> getSpiderPatrols() {
        return Arrays.asList(
            
            // Spider 1: Bottom-left chamber - guards the starting area
            SpiderPatrolData.rectangle(5, 70, 15, 82)
                .describe("Starting chamber guard"),
            
            // Spider 2: Bottom-right vertical corridor
            SpiderPatrolData.vertical(35, 65, 82)
                .describe("Right corridor guard"),
            
            // Spider 3: Middle section - horizontal patrol
            SpiderPatrolData.horizontal(10, 40, 50)
                .describe("Middle horizontal patrol"),
            
            // Spider 4: Central chamber - rectangle patrol
            SpiderPatrolData.rectangle(18, 35, 32, 48)
                .describe("Central chamber patrol"),
            
            // Spider 5: Upper-left area
            SpiderPatrolData.rectangle(5, 20, 15, 32)
                .describe("Upper-left chamber"),
            
            // Spider 6: Upper-right area
            SpiderPatrolData.rectangle(35, 15, 45, 28)
                .describe("Upper-right chamber"),
            
            // Spider 7: Top corridor - guards the exit path
            SpiderPatrolData.horizontal(20, 45, 10)
                .describe("Top corridor guard"),
            
            // Spider 8: Vertical guard near center
            SpiderPatrolData.vertical(25, 25, 45)
                .describe("Center vertical patrol")
        );
    }
    
    // ============================================================
    //                       SNAILS
    // ============================================================
    
    @Override
    public List<SnailLocationData> getSnailLocations() {
        return Arrays.asList(
            
            // Location 1: Starting area - introduces dash
            SnailLocationData.at(784, 2496)
                .withDialogue(
                    "Welcome to the third floor!",
                    "You can now DASH with SHIFT!",
                    "Use it to escape spiders quickly!"
                )
                .requiresInteraction()
                .describe("Dash tutorial"),
            
            // Location 2: First speed boost food
            SnailLocationData.at(464, 2208)
                .withDialogue(
                    "This green seed gives you",
                    "a SPEED BOOST!",
                    "Eat it before running!"
                )
                .requiresInteraction()
                .describe("Energy seed tutorial"),
            
            // Location 3: Middle section - combining dash and speed
            SnailLocationData.at(1200, 1600)
                .withDialogue(
                    "Speed boost + Dash = SUPER SPEED!",
                    "Chain them together!",
                    "It helps in tight situations!"
                )
                .requiresInteraction()
                .describe("Speed combo tip"),
            
            // Location 4: Near exit - final challenge area
            SnailLocationData.at(1472, 384)
                .withDialogue(
                    "The spiders are thick ahead...",
                    "Use your dash wisely!",
                    "Good luck, little one!"
                )
                .requiresInteraction()
                .describe("Final challenge warning")
        );
    }
    
    // ============================================================
    //                        FOOD
    // ============================================================
    //
    // Mix of berries and energy seeds throughout the level
    // Energy seeds are placed in risky but rewarding locations
    //
    // ============================================================
    
    @Override
    public List<FoodSpawnData> getFoodSpawns() {
        return Arrays.asList(
            
            // Bottom area - berries
            FoodSpawnData.berry(8, 75).describe("Bottom-left"),
            FoodSpawnData.berry(20, 78).describe("Bottom-center"),
            FoodSpawnData.berry(38, 72).describe("Bottom-right"),
            
            // Lower-middle section - includes energy seeds
            FoodSpawnData.berry(12, 60).describe("Lower-middle-left"),
            FoodSpawnData.energySeed(25, 62).describe("Risk reward - center"),
            FoodSpawnData.berry(40, 58).describe("Lower-middle-right"),
            
            // Middle section
            FoodSpawnData.berry(8, 48).describe("Middle-left"),
            FoodSpawnData.berry(22, 45).describe("Middle-center"),
            FoodSpawnData.energySeed(35, 50).describe("Middle-right energy"),
            FoodSpawnData.berry(45, 42).describe("Far-right"),
            
            // Upper-middle section
            FoodSpawnData.energySeed(15, 35).describe("Upper-mid energy"),
            FoodSpawnData.berry(28, 32).describe("Upper-middle"),
            FoodSpawnData.berry(42, 38).describe("Upper-mid-right"),
            
            // Upper section
            FoodSpawnData.berry(8, 22).describe("Upper-left"),
            FoodSpawnData.berry(25, 20).describe("Upper-center"),
            FoodSpawnData.energySeed(40, 25).describe("Upper-right energy"),
            
            // Top area - near exit
            FoodSpawnData.berry(15, 12).describe("Top-left"),
            FoodSpawnData.berry(30, 8).describe("Top-center"),
            FoodSpawnData.berry(44, 15).describe("Top-right")
        );
    }
}
