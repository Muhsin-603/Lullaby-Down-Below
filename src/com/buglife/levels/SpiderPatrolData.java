package com.buglife.levels;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

/**
 * Defines a spider's patrol path using TILE coordinates.
 * 
 * USAGE: Create patrol paths like this:
 * 
 *   // Simple back-and-forth patrol (horizontal)
 *   SpiderPatrolData.horizontal(5, 10, 15)  // Row 10, columns 5 to 15
 *   
 *   // Simple back-and-forth patrol (vertical)
 *   SpiderPatrolData.vertical(8, 5, 20)     // Column 8, rows 5 to 20
 *   
 *   // Rectangular patrol (clockwise)
 *   SpiderPatrolData.rectangle(2, 2, 10, 8) // Top-left (2,2), bottom-right (10,8)
 *   
 *   // Custom patrol path
 *   SpiderPatrolData.custom()
 *       .addPoint(5, 5)
 *       .addPoint(10, 5)
 *       .addPoint(10, 10)
 *       .addPoint(5, 10)
 *       .build()
 */
public class SpiderPatrolData {
    
    private final List<Point> waypoints;
    private String description; // Optional: for documentation
    
    private SpiderPatrolData(List<Point> waypoints) {
        this.waypoints = waypoints;
    }
    
    // ========== EASY PATROL BUILDERS ==========
    
    /**
     * Create a horizontal patrol (left-right-left).
     * @param startX Left tile X
     * @param endX Right tile X
     * @param y Row (tile Y)
     */
    public static SpiderPatrolData horizontal(int startX, int endX, int y) {
        List<Point> points = new ArrayList<>();
        points.add(new Point(startX, y));
        points.add(new Point(endX, y));
        points.add(new Point(startX, y)); // Return to start
        return new SpiderPatrolData(points);
    }
    
    /**
     * Create a vertical patrol (up-down-up).
     * @param x Column (tile X)
     * @param startY Top tile Y
     * @param endY Bottom tile Y
     */
    public static SpiderPatrolData vertical(int x, int startY, int endY) {
        List<Point> points = new ArrayList<>();
        points.add(new Point(x, startY));
        points.add(new Point(x, endY));
        points.add(new Point(x, startY)); // Return to start
        return new SpiderPatrolData(points);
    }
    
    /**
     * Create a rectangular patrol (clockwise loop).
     * @param left Left edge tile X
     * @param top Top edge tile Y
     * @param right Right edge tile X
     * @param bottom Bottom edge tile Y
     */
    public static SpiderPatrolData rectangle(int left, int top, int right, int bottom) {
        List<Point> points = new ArrayList<>();
        points.add(new Point(left, top));      // Top-left
        points.add(new Point(right, top));     // Top-right
        points.add(new Point(right, bottom));  // Bottom-right
        points.add(new Point(left, bottom));   // Bottom-left
        points.add(new Point(left, top));      // Back to start
        return new SpiderPatrolData(points);
    }
    
    /**
     * Start building a custom patrol path.
     */
    public static CustomBuilder custom() {
        return new CustomBuilder();
    }
    
    // ========== CUSTOM BUILDER ==========
    
    public static class CustomBuilder {
        private final List<Point> points = new ArrayList<>();
        
        /** Add a waypoint at tile coordinates (tileX, tileY) */
        public CustomBuilder addPoint(int tileX, int tileY) {
            points.add(new Point(tileX, tileY));
            return this;
        }
        
        /** Finish building and return the patrol data */
        public SpiderPatrolData build() {
            // Optionally close the loop if not already closed
            if (!points.isEmpty()) {
                Point first = points.get(0);
                Point last = points.get(points.size() - 1);
                if (!first.equals(last)) {
                    points.add(new Point(first.x, first.y));
                }
            }
            return new SpiderPatrolData(points);
        }
    }
    
    // ========== DESCRIPTION (Optional) ==========
    
    /** Add a description for documentation purposes */
    public SpiderPatrolData describe(String description) {
        this.description = description;
        return this;
    }
    
    public String getDescription() {
        return description;
    }
    
    // ========== GETTER ==========
    
    public List<Point> getWaypoints() {
        return waypoints;
    }
}
