package com.buglife.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Game-wide constants for gameplay mechanics and balance.
 * Values are loaded from config.json at startup, with hardcoded defaults as fallback.
 */
public final class GameConstants {
    private static final Logger logger = LoggerFactory.getLogger(GameConstants.class);
    private static boolean initialized = false;
    
    // Prevent instantiation
    private GameConstants() {
        throw new AssertionError("Constants class should not be instantiated");
    }
    
    /**
     * Initialize all constants from config. Call this once at game startup.
     */
    public static void initialize() {
        if (initialized) return;
        
        ConfigManager config = ConfigManager.getInstance();
        
        // Load Player constants
        Player.NORMAL_SPEED = config.getDouble("player.normalSpeed", 1.8);
        Player.SLOW_SPEED = config.getDouble("player.slowSpeed", 0.5);
        Player.BOOST_SPEED = config.getDouble("player.boostSpeed", 4.0);
        Player.MAX_HUNGER = config.getInt("player.maxHunger", 100);
        Player.HUNGER_DRAIN_INTERVAL = config.getInt("player.hungerDrainInterval", 120);
        Player.DASH_SPEED = config.getInt("player.dashSpeed", 8);
        Player.DASH_DURATION = config.getInt("player.dashDuration", 15);
        Player.DASH_COOLDOWN = config.getInt("player.dashCooldown", 60);
        Player.DASH_HUNGER_COST = config.getInt("player.dashHungerCost", 15);
        
        // Load Spider constants
        Spider.PATROL_SPEED = config.getDouble("spider.patrolSpeed", 1.0);
        Spider.CHASE_SPEED = config.getDouble("spider.chaseSpeed", 3.0);
        Spider.SLOW_CHASE_SPEED = config.getDouble("spider.slowChaseSpeed", 1.7);
        Spider.INVESTIGATE_SPEED = config.getDouble("spider.investigateSpeed", 3.5);
        Spider.DETECTION_RADIUS = config.getInt("spider.detectionRadius", 200);
        Spider.LOSE_SIGHT_DURATION = config.getInt("spider.loseSightDuration", 300);
        
        logger.info("GameConstants initialized from config.json");
        logger.info("Player speeds - Normal: {}, Slow: {}, Boost: {}, Dash: {}", 
            Player.NORMAL_SPEED, Player.SLOW_SPEED, Player.BOOST_SPEED, Player.DASH_SPEED);
        
        initialized = true;
    }
    
    /**
     * Force reload of constants from config (useful for hot-reload during development)
     */
    public static void reload() {
        initialized = false;
        ConfigManager.getInstance().reload();
        initialize();
        logger.info("GameConstants reloaded from config.json");
    }
    
    // === DISPLAY SETTINGS ===
    public static final int VIRTUAL_WIDTH = 1366;
    public static final int VIRTUAL_HEIGHT = 768;
    public static final int TARGET_FPS = 60;
    public static final long NANOS_PER_FRAME = 1_000_000_000L / TARGET_FPS;
    
    // === PLAYER CONSTANTS ===
    public static final class Player {
        // Configurable values (loaded from config.json)
        public static double NORMAL_SPEED = 1.8;
        public static double SLOW_SPEED = 0.5;
        public static double BOOST_SPEED = 4.0;
        public static int MAX_HUNGER = 100;
        public static int HUNGER_DRAIN_INTERVAL = 120;
        public static int DASH_SPEED = 8;
        public static int DASH_DURATION = 15;
        public static int DASH_COOLDOWN = 60;
        public static int DASH_HUNGER_COST = 15;
        
        // Fixed constants (not configurable)
        public static final int HUNGER_DEATH_THRESHOLD = 0;
        public static final int LOW_HUNGER_THRESHOLD = 0;
        public static final int CRY_DEATH_DURATION = 20 * 60;
        public static final int WEB_ESCAPE_REQUIRED = 4;
        public static final int WEBBED_DEATH_TIMER = 300;
        public static final int WEB_IMMUNITY_DURATION = 90; // Added 1.5s of immunity after struggle
        public static final int ANIMATION_SPEED = 3;
        public static final int DRAW_SIZE = 32;
        public static final int COLLISION_SIZE = 32;
        public static final int START_X = 594;
        public static final int START_Y = 2484;
    }
    
    // === SPIDER CONSTANTS ===
    public static final class Spider {
        // Configurable values (loaded from config.json)
        public static double PATROL_SPEED = 1.0;
        public static double CHASE_SPEED = 3.0;
        public static double SLOW_CHASE_SPEED = 1.7;
        public static double INVESTIGATE_SPEED = 3.5;
        public static int DETECTION_RADIUS = 200;
        public static int LOSE_SIGHT_DURATION = 300;
        
        // Fixed constants
        public static final int CAPTURE_RADIUS = 80;
        public static final int INVESTIGATION_DURATION = 300;
        public static final int WIDTH = 48;
        public static final int HEIGHT = 48;
        public static final int ANIMATION_SPEED = 5;
    }
    
    // === WORLD CONSTANTS ===
    public static final class World {
        public static final int TILE_SIZE = 64;
        public static final String DEFAULT_LEVEL = "/res/maps/level1.txt";
        public static final int MAX_TILE_TYPES = 50;
    }
    
    // === FOOD CONSTANTS ===
    public static final class Food {
        public static final int BERRY_HUNGER_VALUE = 20;
        public static final int ENERGY_SEED_HUNGER_VALUE = 15;
        public static final int ENERGY_BOOST_DURATION = 300;
        public static final int SIZE = 16;
    }
    
    // === TOY CONSTANTS ===
    public static final class Toy {
        public static final int NOISE_DURATION = 180;
        public static final int NOISE_RADIUS = 300;
        public static final int SIZE = 20;
    }
    
    // === TRIP WIRE CONSTANTS ===
    public static final class TripWire {
        public static final int SOUND_RADIUS = 200;
        public static final int WIDTH = 32;
        public static final int HEIGHT = 32;
    }
    
    // === SNAIL CONSTANTS ===
    public static final class Snail {
        public static final int WIDTH = 64;
        public static final int HEIGHT = 64;
        public static final int ANIMATION_SPEED = 10;
    }
    
    // === CAMERA CONSTANTS ===
    public static final class Camera {
        public static final int DEAD_ZONE_WIDTH = 200;
        public static final int DEAD_ZONE_HEIGHT = 150;
        public static final double SMOOTH_FACTOR = 0.1;
    }
    
    // === PERFORMANCE ===
    public static final class Performance {
        public static final int MAX_ENTITIES = 1000;
        public static final int QUADTREE_MAX_OBJECTS = 10;
        public static final int QUADTREE_MAX_LEVELS = 5;
    }
}
