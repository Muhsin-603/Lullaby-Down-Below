package com.buglife.levels;

import java.awt.Point;

/**
 * Defines a snail's location with dialogue.
 * 
 * USAGE: Create snail locations like this:
 * 
 *   // Snail that requires player interaction (press E)
 *   SnailLocationData.at(534, 2464)
 *       .withDialogue("Hello little one...", "Be careful!")
 *       .requiresInteraction()
 *   
 *   // Snail that auto-advances dialogue (no interaction needed)
 *   SnailLocationData.at(100, 200)
 *       .withDialogue("Welcome!", "Good luck!")
 *       .autoAdvance()
 *   
 *   // Using tile coordinates instead of pixels
 *   SnailLocationData.atTile(15, 20)
 *       .withDialogue("You found me!")
 *       .requiresInteraction()
 */
public class SnailLocationData {
    
    private static final int TILE_SIZE = 32; // Match World.TILE_SIZE
    
    private final Point position; // Pixel coordinates
    private String[] dialogue;
    private boolean requiresInteraction = true; // Default: requires E key
    private String description; // Optional: for documentation
    
    private SnailLocationData(Point position) {
        this.position = position;
        this.dialogue = new String[] { "..." }; // Default dialogue
    }
    
    // ========== POSITION BUILDERS ==========
    
    /**
     * Create a snail at pixel coordinates.
     * @param pixelX X position in pixels
     * @param pixelY Y position in pixels
     */
    public static SnailLocationData at(int pixelX, int pixelY) {
        return new SnailLocationData(new Point(pixelX, pixelY));
    }
    
    /**
     * Create a snail at tile coordinates (converts to pixels).
     * @param tileX Tile X coordinate
     * @param tileY Tile Y coordinate
     */
    public static SnailLocationData atTile(int tileX, int tileY) {
        int pixelX = tileX * TILE_SIZE + (TILE_SIZE / 2);
        int pixelY = tileY * TILE_SIZE + (TILE_SIZE / 2);
        return new SnailLocationData(new Point(pixelX, pixelY));
    }
    
    // ========== DIALOGUE ==========
    
    /**
     * Set the dialogue lines for this snail location.
     * @param lines One or more dialogue strings
     */
    public SnailLocationData withDialogue(String... lines) {
        this.dialogue = lines;
        return this;
    }
    
    // ========== INTERACTION TYPE ==========
    
    /** Player must press E to advance dialogue and let snail teleport */
    public SnailLocationData requiresInteraction() {
        this.requiresInteraction = true;
        return this;
    }
    
    /** Dialogue auto-advances, snail teleports when off-screen */
    public SnailLocationData autoAdvance() {
        this.requiresInteraction = false;
        return this;
    }
    
    // ========== DESCRIPTION (Optional) ==========
    
    /** Add a description for documentation purposes */
    public SnailLocationData describe(String description) {
        this.description = description;
        return this;
    }
    
    public String getDescription() {
        return description;
    }
    
    // ========== GETTERS ==========
    
    public Point getPosition() {
        return position;
    }
    
    public String[] getDialogue() {
        return dialogue;
    }
    
    /**
     * Check if this snail requires player interaction (E key).
     * Named differently from the builder method to avoid conflict.
     */
    public boolean isInteractionRequired() {
        return requiresInteraction;
    }
}
