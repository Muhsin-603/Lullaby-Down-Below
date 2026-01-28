package com.buglife.config;

/**
 * Game-wide constants for gameplay mechanics and balance.
 * All magic numbers should be defined here for easy tuning.
 */
public final class GameConstants {
    
    // Prevent instantiation
    private GameConstants() {
        throw new AssertionError("Constants class should not be instantiated");
    }
    
    // === DISPLAY SETTINGS ===
    public static final int VIRTUAL_WIDTH = 1366;
    public static final int VIRTUAL_HEIGHT = 768;
    public static final int TARGET_FPS = 60;
    public static final long NANOS_PER_FRAME = 1_000_000_000L / TARGET_FPS;
    
    // === PLAYER CONSTANTS ===
    public static final class Player {
        public static final double NORMAL_SPEED = 1.8;
        public static final double SLOW_SPEED = 0.5;
        public static final double BOOST_SPEED = 4.0;
        public static final int MAX_HUNGER = 100;
        public static final int HUNGER_DRAIN_INTERVAL = 120; // frames (2 seconds at 60fps)
        public static final int HUNGER_DEATH_THRESHOLD = 0;
        public static final int LOW_HUNGER_THRESHOLD = 0;
        public static final int CRY_DEATH_DURATION = 20 * 60; // 20 seconds
        
        // Dash mechanics
        public static final int DASH_SPEED = 8;
        public static final int DASH_DURATION = 15; // frames
        public static final int DASH_COOLDOWN = 60; // frames
        public static final int DASH_HUNGER_COST = 15;
        
        // Webbed mechanics
        public static final int WEB_ESCAPE_REQUIRED = 4; // button presses
        public static final int WEBBED_DEATH_TIMER = 300; // frames (5 seconds)
        
        // Animation
        public static final int ANIMATION_SPEED = 3; // frames per sprite change
        
        // Size
        public static final int DRAW_SIZE = 32;
        public static final int COLLISION_SIZE = 32;
        
        // Starting position
        public static final int START_X = 594;
        public static final int START_Y = 2484;
    }
    
    // === SPIDER CONSTANTS ===
    public static final class Spider {
        public static final double PATROL_SPEED = 1.0;
        public static final double CHASE_SPEED = 3.0;
        public static final double SLOW_CHASE_SPEED = 1.7;
        public static final double INVESTIGATE_SPEED = 3.5;
        
        public static final int DETECTION_RADIUS = 200; // pixels
        public static final int CAPTURE_RADIUS = 80; // pixels
        public static final int LOSE_SIGHT_DURATION = 300; // frames (5 seconds)
        public static final int INVESTIGATION_DURATION = 300; // frames
        
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
        public static final int ENERGY_BOOST_DURATION = 300; // frames (5 seconds)
        
        public static final int SIZE = 16;
    }
    
    // === TOY CONSTANTS ===
    public static final class Toy {
        public static final int NOISE_DURATION = 180; // frames (3 seconds)
        public static final int NOISE_RADIUS = 300; // pixels
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
