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
    
    /**
     * Get the configuration for a level by name.
     * Creates a fresh instance each time to support live-reload workflows.
     * 
     * @param levelName The level name (e.g., "level1", "level2", "level_test")
     * @return The LevelConfig for that level, or Level1Config as fallback
     */
    public static LevelConfig getConfig(String levelName) {
        switch (levelName) {
            case "level1":
                return new Level1Config();
                
            case "level2":
                return new Level2Config();
                
            case "level3":
                return new Level3Config();
                
            case "level4":
                return new Level4Config();
                
            case "level5":
                return new Level5Config();
                
            case "level_test":
                return new LevelTestConfig();
                
            default:
                // Unknown level - fall back to Level 1
                System.err.println("[LevelConfigFactory] Unknown level: " + levelName + ", using Level1Config");
                return new Level1Config();
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
