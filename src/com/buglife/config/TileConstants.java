package com.buglife.config;

/**
 * Constants for tile IDs and properties.
 * Maps tile IDs to their semantic meaning.
 */
public final class TileConstants {
    
    private TileConstants() {
        throw new AssertionError("Constants class should not be instantiated");
    }
    
    // === BASIC TILES ===
    public static final int FLOOR = 0;
    public static final int WALL = 1;
    public static final int WALL_ALT = 2;
    public static final int STICKY_FLOOR = 3;
    public static final int BROKEN_TILE = 4;
    public static final int SHADOW_TILE = 5;
    
    // === STAIN FLOORS ===
    public static final int STAIN_1 = 6;
    public static final int STAIN_2 = 7;
    public static final int STAIN_3 = 8;
    public static final int STAIN_4 = 9;
    
    // === SACK PROP TILES ===
    public static final int SACK_W1 = 11;
    public static final int SACK_W2 = 12;
    public static final int SACK_W3 = 13;
    public static final int SACK_W4 = 14;
    
    // === PLANK TILES ===
    public static final int PLANK_1 = 31;
    public static final int PLANK_2 = 32;
    public static final int PLANK_3 = 33;
    public static final int PLANK_4 = 34;
    
    // === LADDER TILES ===
    public static final int LADDER_1 = 35;
    public static final int LADDER_2 = 36;
    public static final int LADDER_3 = 37; // Level complete tile
    public static final int LADDER_4 = 38;
    
    // === CHIMNEY INTRO TILES ===
    public static final int INTRO_TILE_1 = 41;
    public static final int INTRO_TILE_2 = 42;
    public static final int INTRO_TILE_3 = 43;
    public static final int INTRO_TILE_4 = 44;
    public static final int INTRO_TILE_5 = 45;
    public static final int INTRO_TILE_6 = 46;
    
    // === SPECIAL TILES ===
    public static final int LEVEL_COMPLETE_TILE = LADDER_3;
    
    /**
     * Check if a tile ID represents a solid/collidable tile
     */
    public static boolean isSolidTile(int tileId) {
        return tileId == WALL 
            || tileId == WALL_ALT 
            || tileId == BROKEN_TILE
            || tileId == SACK_W1
            || tileId == SACK_W2
            || tileId == SACK_W4
            || tileId == LADDER_1
            || tileId == LADDER_2
            || tileId == LADDER_4
            || tileId == INTRO_TILE_4
            || tileId == INTRO_TILE_5
            || tileId == INTRO_TILE_6;
    }
    
    /**
     * Check if a tile ID represents a slow-down tile
     */
    public static boolean isSlowTile(int tileId) {
        return tileId == STICKY_FLOOR;
    }
    
    /**
     * Check if a tile ID represents a shadow/transparent tile
     */
    public static boolean isShadowTile(int tileId) {
        return tileId == SHADOW_TILE;
    }
    
    /**
     * Get human-readable name for a tile ID
     */
    public static String getTileName(int tileId) {
        switch (tileId) {
            case FLOOR: return "Floor";
            case WALL: return "Wall";
            case WALL_ALT: return "Wall (Alt)";
            case STICKY_FLOOR: return "Sticky Floor";
            case BROKEN_TILE: return "Broken Tile";
            case SHADOW_TILE: return "Shadow";
            case STAIN_1: return "Stain 1";
            case STAIN_2: return "Stain 2";
            case STAIN_3: return "Stain 3";
            case STAIN_4: return "Stain 4";
            case SACK_W1: return "Sack W1";
            case SACK_W2: return "Sack W2";
            case SACK_W3: return "Sack W3";
            case SACK_W4: return "Sack W4";
            case PLANK_1: return "Plank 1";
            case PLANK_2: return "Plank 2";
            case PLANK_3: return "Plank 3";
            case PLANK_4: return "Plank 4";
            case LADDER_1: return "Ladder 1";
            case LADDER_2: return "Ladder 2";
            case LADDER_3: return "Ladder 3 (Exit)";
            case LADDER_4: return "Ladder 4";
            case INTRO_TILE_1: return "Intro 1";
            case INTRO_TILE_2: return "Intro 2";
            case INTRO_TILE_3: return "Intro 3";
            case INTRO_TILE_4: return "Intro 4";
            case INTRO_TILE_5: return "Intro 5";
            case INTRO_TILE_6: return "Intro 6";
            default: return "Tile " + tileId;
        }
    }
    
    /**
     * Check if a tile ID is a valid exit/level-complete tile
     */
    public static boolean isExitTile(int tileId) {
        return tileId == LEVEL_COMPLETE_TILE;
    }
    
    // Common aliases for editor compatibility
    public static final int AIR = FLOOR;
    public static final int GROUND = WALL;
    public static final int PLATFORM = WALL_ALT;
    public static final int EXIT = LEVEL_COMPLETE_TILE;
    
    // Wall direction aliases (map to existing walls)
    public static final int WALL_LEFT = WALL;
    public static final int WALL_RIGHT = WALL;
    public static final int WALL_BOTTOM = WALL;
    public static final int CORNER_TOP_LEFT = WALL;
    public static final int CORNER_TOP_RIGHT = WALL;
    public static final int CORNER_BOTTOM_LEFT = WALL;
    public static final int CORNER_BOTTOM_RIGHT = WALL;
    public static final int SLOPE_LEFT = FLOOR;
    public static final int SLOPE_RIGHT = FLOOR;
    public static final int CEILING_SLOPE_LEFT = WALL;
    public static final int CEILING_SLOPE_RIGHT = WALL;
}
