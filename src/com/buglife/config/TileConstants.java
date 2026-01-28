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
}
