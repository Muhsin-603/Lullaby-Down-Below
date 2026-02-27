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
 * Fourth floor - introduces TOY and TRIPWIRES mechanics.
 * 
 * ENABLED MECHANICS:
 *   - Toy (pick up with E, throw with F) - Distract spiders
 *   - Tripwires - Create distractions automatically
 * 
 * DISABLED MECHANICS:
 *   - No dash ability
 *   - No speed boost food
 * 
 * This level teaches players to use the toy as a distraction
 * and navigate through tripwire-laden corridors.
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
        // Starting position - left side of the map
        return new Point(96, 2624);
    }
    
    // ============================================================
    //                      MECHANICS
    // ============================================================
    
    @Override
    public MechanicsConfig getMechanicsEnabled() {
        // Level 4 introduces toy and tripwires
        return new MechanicsConfig()
            .enableToy()
            .enableTripWires();
    }
    
    // ============================================================
    //                     TOY SPAWN
    // ============================================================
    
    @Override
    public Point getToySpawn() {
        // Toy spawns near starting area - encourage early use
        return new Point(352, 2528);
    }
    
    // ============================================================
    //                     TRIPWIRES
    // ============================================================
    //
    // Tripwires are placed at strategic chokepoints
    // They trigger webs to distract nearby spiders
    //
    // ============================================================
    
    @Override
    public List<Point> getTripWirePositions() {
        return Arrays.asList(
            // Bottom corridor tripwires
            new Point(400, 2560),
            new Point(700, 2560),
            new Point(1000, 2560),
            
            // Middle section tripwires
            new Point(300, 1984),
            new Point(600, 1984),
            new Point(950, 1856),
            
            // Upper section tripwires
            new Point(450, 1408),
            new Point(800, 1280),
            new Point(1100, 1152),
            
            // Near exit area
            new Point(600, 640),
            new Point(1000, 512)
        );
    }
    
    // ============================================================
    //                      SPIDERS
    // ============================================================
    //
    // Level 4 map is 50 columns (0-49) x 88 rows (0-87)
    // More spiders with larger patrol ranges
    //
    // ============================================================
    
    @Override
    public List<SpiderPatrolData> getSpiderPatrols() {
        return Arrays.asList(
            
            // Spider 1: Bottom-left - near spawn
            SpiderPatrolData.rectangle(3, 70, 12, 82)
                .describe("Spawn area guard"),
            
            // Spider 2: Bottom corridor - horizontal
            SpiderPatrolData.horizontal(8, 30, 75)
                .describe("Bottom corridor patrol"),
            
            // Spider 3: Right side vertical
            SpiderPatrolData.vertical(42, 60, 80)
                .describe("Right side guard"),
            
            // Spider 4: Lower-middle chamber
            SpiderPatrolData.rectangle(15, 50, 25, 65)
                .describe("Lower-middle chamber"),
            
            // Spider 5: Central area - large patrol
            SpiderPatrolData.rectangle(8, 35, 40, 50)
                .describe("Central large patrol"),
            
            // Spider 6: Middle-left
            SpiderPatrolData.vertical(12, 30, 48)
                .describe("Middle-left vertical"),
            
            // Spider 7: Middle-right
            SpiderPatrolData.vertical(38, 28, 45)
                .describe("Middle-right vertical"),
            
            // Spider 8: Upper-middle area
            SpiderPatrolData.rectangle(15, 18, 35, 30)
                .describe("Upper-middle patrol"),
            
            // Spider 9: Upper-left chamber
            SpiderPatrolData.rectangle(3, 10, 15, 22)
                .describe("Upper-left chamber"),
            
            // Spider 10: Upper-right chamber
            SpiderPatrolData.rectangle(35, 8, 47, 20)
                .describe("Upper-right chamber"),
            
            // Spider 11: Top corridor - exit guard
            SpiderPatrolData.horizontal(20, 45, 8)
                .describe("Exit corridor guard"),
            
            // Spider 12: Vertical guard near center
            SpiderPatrolData.vertical(25, 15, 35)
                .describe("Center vertical patrol")
        );
    }
    
    // ============================================================
    //                       SNAILS
    // ============================================================
    
    @Override
    public List<SnailLocationData> getSnailLocations() {
        return Arrays.asList(
            
            // Location 1: Starting area - introduces toy
            SnailLocationData.at(160, 2496)
                .withDialogue(
                    "Welcome to the fourth floor!",
                    "You can pick up the TOY with E",
                    "And throw it with F to distract spiders!"
                )
                .requiresInteraction()
                .describe("Toy tutorial"),
            
            // Location 2: Tripwire introduction
            SnailLocationData.at(480, 2208)
                .withDialogue(
                    "See those glowing lines?",
                    "They're TRIPWIRES!",
                    "They create webs to distract spiders!"
                )
                .requiresInteraction()
                .describe("Tripwire tutorial"),
            
            // Location 3: Toy + Tripwire combo
            SnailLocationData.at(864, 1728)
                .withDialogue(
                    "Combine toy and tripwires!",
                    "Lead spiders to tripwires",
                    "Then throw the toy elsewhere!"
                )
                .requiresInteraction()
                .describe("Combo strategy"),
            
            // Location 4: Upper section
            SnailLocationData.at(704, 896)
                .withDialogue(
                    "The path ahead is dangerous...",
                    "Use the toy to create diversions!",
                    "Stay hidden when you can!"
                )
                .requiresInteraction()
                .describe("Upper section tip"),
            
            // Location 5: Near exit
            SnailLocationData.at(1184, 320)
                .withDialogue(
                    "Almost there!",
                    "One more floor to go!",
                    "You've learned well, little one!"
                )
                .requiresInteraction()
                .describe("Final stretch encouragement")
        );
    }
    
    // ============================================================
    //                        FOOD
    // ============================================================
    //
    // Food placed to encourage exploration and toy usage
    //
    // ============================================================
    
    @Override
    public List<FoodSpawnData> getFoodSpawns() {
        return Arrays.asList(
            
            // Bottom area
            FoodSpawnData.berry(5, 76).describe("Bottom-left"),
            FoodSpawnData.berry(18, 78).describe("Bottom-center-left"),
            FoodSpawnData.berry(32, 74).describe("Bottom-center-right"),
            FoodSpawnData.berry(45, 70).describe("Bottom-right"),
            
            // Lower-middle
            FoodSpawnData.berry(10, 60).describe("Lower-mid-left"),
            FoodSpawnData.berry(25, 58).describe("Lower-mid-center"),
            FoodSpawnData.berry(40, 62).describe("Lower-mid-right"),
            
            // Middle section
            FoodSpawnData.berry(6, 45).describe("Middle-left"),
            FoodSpawnData.berry(20, 42).describe("Middle-center-left"),
            FoodSpawnData.berry(35, 48).describe("Middle-center-right"),
            FoodSpawnData.berry(48, 40).describe("Middle-right"),
            
            // Upper-middle
            FoodSpawnData.berry(8, 30).describe("Upper-mid-left"),
            FoodSpawnData.berry(22, 28).describe("Upper-mid-center"),
            FoodSpawnData.berry(38, 32).describe("Upper-mid-right"),
            FoodSpawnData.berry(45, 25).describe("Upper-mid-far-right"),
            
            // Upper section
            FoodSpawnData.berry(5, 15).describe("Upper-left"),
            FoodSpawnData.berry(18, 12).describe("Upper-center-left"),
            FoodSpawnData.berry(32, 18).describe("Upper-center-right"),
            FoodSpawnData.berry(44, 10).describe("Upper-right"),
            
            // Top area - near exit
            FoodSpawnData.berry(12, 5).describe("Top-left"),
            FoodSpawnData.berry(28, 3).describe("Top-center"),
            FoodSpawnData.berry(42, 6).describe("Top-right")
        );
    }
}
