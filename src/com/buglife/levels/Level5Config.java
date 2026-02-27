package com.buglife.levels;

import java.awt.Point;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * ============================================================
 *                        LEVEL 5
 * ============================================================
 * 
 * Final floor - ALL MECHANICS ENABLED!
 * 
 * ENABLED MECHANICS:
 *   - Dash ability (SHIFT key) - Quick burst of speed
 *   - Toy (pick up with E, throw with F) - Distract spiders
 *   - Tripwires - Create distractions automatically
 *   - Speed boost food (ENERGY_SEED) - Gives temporary speed
 * 
 * This is the ultimate challenge - combines everything learned!
 * Expect heavy spider presence and complex navigation.
 */
public class Level5Config implements LevelConfig {

    @Override
    public String getLevelName() {
        return "level5";
    }
    
    // ============================================================
    //                     PLAYER SPAWN
    // ============================================================
    
    @Override
    public Point getPlayerSpawn() {
        // Starting position - bottom center
        return new Point(784, 2624);
    }
    
    // ============================================================
    //                      MECHANICS
    // ============================================================
    
    @Override
    public MechanicsConfig getMechanicsEnabled() {
        // Level 5 has ALL mechanics enabled!
        return new MechanicsConfig()
            .enableDash()
            .enableToy()
            .enableTripWires()
            .enableSpeedBoostFood();
    }
    
    // ============================================================
    //                     TOY SPAWN
    // ============================================================
    
    @Override
    public Point getToySpawn() {
        // Toy spawns in a central location
        return new Point(784, 2200);
    }
    
    // ============================================================
    //                     TRIPWIRES
    // ============================================================
    //
    // Heavy tripwire placement for the final challenge
    // Strategic positions at major chokepoints
    //
    // ============================================================
    
    @Override
    public List<Point> getTripWirePositions() {
        return Arrays.asList(
            // Bottom section
            new Point(200, 2592),
            new Point(500, 2624),
            new Point(1100, 2592),
            new Point(1400, 2560),
            
            // Lower-middle section
            new Point(300, 2208),
            new Point(650, 2240),
            new Point(1000, 2176),
            new Point(1350, 2208),
            
            // Middle section
            new Point(150, 1856),
            new Point(450, 1824),
            new Point(750, 1856),
            new Point(1100, 1792),
            new Point(1450, 1824),
            
            // Upper-middle section
            new Point(250, 1440),
            new Point(550, 1472),
            new Point(900, 1408),
            new Point(1200, 1440),
            new Point(1480, 1472),
            
            // Upper section
            new Point(350, 1024),
            new Point(700, 1056),
            new Point(1050, 992),
            new Point(1350, 1024),
            
            // Top section - exit area
            new Point(450, 640),
            new Point(800, 608),
            new Point(1150, 576),
            new Point(1450, 640),
            
            // Near exit
            new Point(600, 288),
            new Point(1000, 256)
        );
    }
    
    // ============================================================
    //                      SPIDERS
    // ============================================================
    //
    // Level 5 map is 50 columns (0-49) x 88 rows (0-87)
    // MAXIMUM spider density for the final challenge!
    //
    // ============================================================
    
    @Override
    public List<SpiderPatrolData> getSpiderPatrols() {
        return Arrays.asList(
            
            // Bottom layer - heavy coverage
            SpiderPatrolData.rectangle(3, 72, 15, 84)
                .describe("Bottom-left guard"),
            
            SpiderPatrolData.horizontal(10, 25, 78)
                .describe("Bottom-center-left patrol"),
            
            SpiderPatrolData.horizontal(25, 40, 75)
                .describe("Bottom-center-right patrol"),
            
            SpiderPatrolData.rectangle(38, 70, 47, 84)
                .describe("Bottom-right guard"),
            
            SpiderPatrolData.vertical(5, 60, 75)
                .describe("Left edge vertical"),
            
            SpiderPatrolData.vertical(45, 62, 78)
                .describe("Right edge vertical"),
            
            // Lower-middle layer
            SpiderPatrolData.rectangle(8, 55, 20, 68)
                .describe("Lower-mid-left chamber"),
            
            SpiderPatrolData.rectangle(28, 52, 42, 66)
                .describe("Lower-mid-right chamber"),
            
            SpiderPatrolData.horizontal(15, 35, 60)
                .describe("Lower-middle horizontal"),
            
            // Middle layer
            SpiderPatrolData.rectangle(3, 40, 18, 52)
                .describe("Mid-left large patrol"),
            
            SpiderPatrolData.rectangle(32, 38, 47, 50)
                .describe("Mid-right large patrol"),
            
            SpiderPatrolData.vertical(25, 35, 55)
                .describe("Center vertical main"),
            
            SpiderPatrolData.horizontal(10, 45, 45)
                .describe("Middle horizontal main"),
            
            // Upper-middle layer
            SpiderPatrolData.rectangle(5, 25, 20, 38)
                .describe("Upper-mid-left patrol"),
            
            SpiderPatrolData.rectangle(28, 22, 45, 36)
                .describe("Upper-mid-right patrol"),
            
            SpiderPatrolData.vertical(15, 20, 40)
                .describe("Mid-left vertical"),
            
            SpiderPatrolData.vertical(38, 18, 42)
                .describe("Mid-right vertical"),
            
            // Upper layer
            SpiderPatrolData.rectangle(8, 10, 22, 22)
                .describe("Upper-left chamber"),
            
            SpiderPatrolData.rectangle(30, 8, 45, 20)
                .describe("Upper-right chamber"),
            
            SpiderPatrolData.horizontal(18, 38, 15)
                .describe("Upper horizontal main"),
            
            SpiderPatrolData.vertical(28, 12, 28)
                .describe("Upper center vertical"),
            
            // Top layer - exit guards
            SpiderPatrolData.rectangle(12, 3, 25, 10)
                .describe("Top-left exit guard"),
            
            SpiderPatrolData.rectangle(32, 2, 47, 12)
                .describe("Top-right exit guard"),
            
            SpiderPatrolData.horizontal(20, 42, 6)
                .describe("Final corridor guard"),
            
            // Extra mobile patrols
            SpiderPatrolData.vertical(20, 45, 65)
                .describe("Mobile vertical 1"),
            
            SpiderPatrolData.vertical(35, 40, 60)
                .describe("Mobile vertical 2"),
            
            SpiderPatrolData.horizontal(8, 30, 50)
                .describe("Mobile horizontal 1"),
            
            SpiderPatrolData.horizontal(25, 45, 35)
                .describe("Mobile horizontal 2")
        );
    }
    
    // ============================================================
    //                       SNAILS
    // ============================================================
    
    @Override
    public List<SnailLocationData> getSnailLocations() {
        return Arrays.asList(
            
            // Location 1: Starting area - final tutorial
            SnailLocationData.at(784, 2528)
                .withDialogue(
                    "This is it - the final floor!",
                    "You have ALL your abilities now:",
                    "Dash, Toy, Tripwires, AND Speed Boost!"
                )
                .requiresInteraction()
                .describe("Final floor intro"),
            
            // Location 2: Toy reminder
            SnailLocationData.at(400, 2112)
                .withDialogue(
                    "Don't forget your Toy!",
                    "Use it to lure spiders",
                    "into tripwires for easy escapes!"
                )
                .requiresInteraction()
                .describe("Toy reminder"),
            
            // Location 3: Speed + Dash combo
            SnailLocationData.at(1000, 1856)
                .withDialogue(
                    "Speed Boost + Dash = ULTIMATE SPEED!",
                    "Use this to cross dangerous areas!",
                    "Chain them for maximum effect!"
                )
                .requiresInteraction()
                .describe("Speed combo reminder"),
            
            // Location 4: Middle section
            SnailLocationData.at(600, 1408)
                .withDialogue(
                    "The spiders are everywhere...",
                    "Combine all your abilities!",
                    "Dash, throw toy, hide in shadows!"
                )
                .requiresInteraction()
                .describe("Mid-level encouragement"),
            
            // Location 5: Upper section
            SnailLocationData.at(1200, 1024)
                .withDialogue(
                    "Almost at the top!",
                    "Stay calm and use strategy!",
                    "You can do this!"
                )
                .requiresInteraction()
                .describe("Upper section motivation"),
            
            // Location 6: Near exit
            SnailLocationData.at(800, 384)
                .withDialogue(
                    "The exit is close!",
                    "One final push, little one!",
                    "You've mastered everything!"
                )
                .requiresInteraction()
                .describe("Final stretch")
        );
    }
    
    // ============================================================
    //                        FOOD
    // ============================================================
    //
    // Generous food placement to help with the intense difficulty
    // Energy seeds in key positions for speed boosts
    //
    // ============================================================
    
    @Override
    public List<FoodSpawnData> getFoodSpawns() {
        return Arrays.asList(
            
            // Bottom row
            FoodSpawnData.berry(4, 80).describe("Bottom-1"),
            FoodSpawnData.berry(15, 82).describe("Bottom-2"),
            FoodSpawnData.energySeed(25, 78).describe("Bottom-energy-1"),
            FoodSpawnData.berry(35, 80).describe("Bottom-3"),
            FoodSpawnData.berry(45, 76).describe("Bottom-4"),
            
            // Lower-middle row
            FoodSpawnData.berry(6, 68).describe("Lower-mid-1"),
            FoodSpawnData.berry(18, 70).describe("Lower-mid-2"),
            FoodSpawnData.energySeed(30, 65).describe("Lower-mid-energy"),
            FoodSpawnData.berry(40, 68).describe("Lower-mid-3"),
            FoodSpawnData.berry(48, 62).describe("Lower-mid-4"),
            
            // Middle section
            FoodSpawnData.berry(4, 55).describe("Mid-1"),
            FoodSpawnData.energySeed(15, 52).describe("Mid-energy-1"),
            FoodSpawnData.berry(25, 50).describe("Mid-2"),
            FoodSpawnData.berry(35, 55).describe("Mid-3"),
            FoodSpawnData.energySeed(45, 48).describe("Mid-energy-2"),
            
            // Upper-middle section
            FoodSpawnData.berry(8, 42).describe("Upper-mid-1"),
            FoodSpawnData.berry(20, 40).describe("Upper-mid-2"),
            FoodSpawnData.energySeed(32, 38).describe("Upper-mid-energy"),
            FoodSpawnData.berry(42, 42).describe("Upper-mid-3"),
            FoodSpawnData.berry(47, 35).describe("Upper-mid-4"),
            
            // Upper section
            FoodSpawnData.berry(5, 28).describe("Upper-1"),
            FoodSpawnData.berry(18, 25).describe("Upper-2"),
            FoodSpawnData.energySeed(28, 30).describe("Upper-energy-1"),
            FoodSpawnData.berry(38, 28).describe("Upper-3"),
            FoodSpawnData.berry(45, 22).describe("Upper-4"),
            
            // Top section
            FoodSpawnData.berry(8, 15).describe("Top-1"),
            FoodSpawnData.berry(22, 12).describe("Top-2"),
            FoodSpawnData.energySeed(35, 18).describe("Top-energy"),
            FoodSpawnData.berry(44, 10).describe("Top-3"),
            
            // Near exit
            FoodSpawnData.berry(12, 5).describe("Exit-1"),
            FoodSpawnData.energySeed(28, 3).describe("Exit-energy"),
            FoodSpawnData.berry(40, 6).describe("Exit-2")
        );
    }
}
