package com.buglife.levels;

/**
 * Factory for getting level configurations.
 * 
 * USAGE:
 *   LevelConfig config = LevelConfigFactory.getConfig("level1");
 * 
 * TO ADD A NEW LEVEL:
 *   1. Create LevelXConfig.java implementing LevelConfig
 *   2. Add a case for it in getConfig() below
 */
public class LevelConfigFactory {
    
    // Cached instances (singleton per level)
    private static final Level1Config LEVEL_1 = new Level1Config();
    private static final Level2Config LEVEL_2 = new Level2Config();
    private static final Level3Config LEVEL_3 = new Level3Config();
    private static final Level4Config LEVEL_4 = new Level4Config();
    private static final Level5Config LEVEL_5 = new Level5Config();
    private static final LevelTestConfig LEVEL_TEST = new LevelTestConfig();
    
    /**
     * Get the configuration for a level by name.
     * 
     * @param levelName The level name (e.g., "level1", "level2", "level_test")
     * @return The LevelConfig for that level, or Level1Config as fallback
     */
    public static LevelConfig getConfig(String levelName) {
        switch (levelName) {
            case "level1":
                return LEVEL_1;
                
            case "level2":
                return LEVEL_2;
                
            case "level3":
                return LEVEL_3;
                
            case "level4":
                return LEVEL_4;
                
            case "level5":
                return LEVEL_5;
                
            case "level_test":
                return LEVEL_TEST;
                
            default:
                // Unknown level - fall back to Level 1
                System.err.println("[LevelConfigFactory] Unknown level: " + levelName + ", using Level1Config");
                return LEVEL_1;
        }
    }
    
    /**
     * Check if a level configuration exists.
     */
    public static boolean hasConfig(String levelName) {
        switch (levelName) {
            case "level1":
            case "level2":
            case "level3":
            case "level4":
            case "level5":
            case "level_test":
                return true;
            default:
                return false;
        }
    }
}
