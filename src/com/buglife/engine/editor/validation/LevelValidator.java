package com.buglife.engine.editor.validation;

import com.buglife.config.TileConstants;
import com.buglife.engine.editor.data.LevelData;
import com.buglife.engine.editor.data.LevelData.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates level data for common errors and design issues.
 * Provides actionable feedback for level designers.
 */
public class LevelValidator {
    
    public enum Severity {
        ERROR,      // Must fix - game will not work correctly
        WARNING,    // Should fix - may cause issues
        INFO        // Suggestion - quality improvement
    }
    
    public static class ValidationIssue {
        public final Severity severity;
        public final String category;
        public final String message;
        public final int entityIndex;  // -1 if not entity-related
        public final String entityType; // null if not entity-related
        public final int tileX, tileY;  // -1 if not tile-related
        
        public ValidationIssue(Severity severity, String category, String message) {
            this(severity, category, message, -1, null, -1, -1);
        }
        
        public ValidationIssue(Severity severity, String category, String message, 
                               int entityIndex, String entityType) {
            this(severity, category, message, entityIndex, entityType, -1, -1);
        }
        
        public ValidationIssue(Severity severity, String category, String message,
                               int tileX, int tileY) {
            this(severity, category, message, -1, null, tileX, tileY);
        }
        
        public ValidationIssue(Severity severity, String category, String message,
                               int entityIndex, String entityType, int tileX, int tileY) {
            this.severity = severity;
            this.category = category;
            this.message = message;
            this.entityIndex = entityIndex;
            this.entityType = entityType;
            this.tileX = tileX;
            this.tileY = tileY;
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("[").append(severity).append("] ");
            sb.append(category).append(": ").append(message);
            if (tileX >= 0) {
                sb.append(" @ tile(").append(tileX).append(",").append(tileY).append(")");
            }
            if (entityType != null) {
                sb.append(" [").append(entityType).append(" #").append(entityIndex + 1).append("]");
            }
            return sb.toString();
        }
    }
    
    private static final int TILE_SIZE = 64;
    
    /**
     * Validate level data and return list of issues.
     */
    public static List<ValidationIssue> validate(LevelData data) {
        List<ValidationIssue> issues = new ArrayList<>();
        
        // === REQUIRED SPAWNS ===
        validateRequiredSpawns(data, issues);
        
        // === EXIT TILE ===
        validateExitTile(data, issues);
        
        // === SPIDERS ===
        validateSpiders(data, issues);
        
        // === SNAILS ===
        validateSnails(data, issues);
        
        // === FOOD ===
        validateFood(data, issues);
        
        // === TRIPWIRES ===
        validateTripwires(data, issues);
        
        // === MAP QUALITY ===
        validateMapQuality(data, issues);
        
        return issues;
    }
    
    private static void validateRequiredSpawns(LevelData data, List<ValidationIssue> issues) {
        // Player spawn is required
        if (data.getPlayerSpawn() == null) {
            issues.add(new ValidationIssue(Severity.ERROR, "Spawn", 
                "Missing player spawn point"));
        } else {
            PointData p = data.getPlayerSpawn();
            int tileX = p.x / TILE_SIZE;
            int tileY = p.y / TILE_SIZE;
            
            // Check if player spawn is in bounds
            if (tileX < 0 || tileX >= data.getWidth() || tileY < 0 || tileY >= data.getHeight()) {
                issues.add(new ValidationIssue(Severity.ERROR, "Spawn",
                    "Player spawn is outside map bounds", tileX, tileY));
            }
            // Check if player spawn is on solid tile
            else if (TileConstants.isSolidTile(data.getTile(tileX, tileY))) {
                issues.add(new ValidationIssue(Severity.ERROR, "Spawn",
                    "Player spawn is on a solid tile", tileX, tileY));
            }
        }
        
        // Toy spawn required if toy enabled
        if (data.getMechanics().toyEnabled) {
            if (data.getToySpawn() == null) {
                issues.add(new ValidationIssue(Severity.ERROR, "Spawn",
                    "Toy mechanics enabled but no toy spawn point set"));
            } else {
                PointData p = data.getToySpawn();
                int tileX = p.x / TILE_SIZE;
                int tileY = p.y / TILE_SIZE;
                
                if (tileX < 0 || tileX >= data.getWidth() || tileY < 0 || tileY >= data.getHeight()) {
                    issues.add(new ValidationIssue(Severity.ERROR, "Spawn",
                        "Toy spawn is outside map bounds", tileX, tileY));
                }
            }
        }
    }
    
    private static void validateExitTile(LevelData data, List<ValidationIssue> issues) {
        // Look for level complete tile (LADDER_3 = 37)
        boolean hasExit = false;
        for (int y = 0; y < data.getHeight(); y++) {
            for (int x = 0; x < data.getWidth(); x++) {
                if (data.getTile(x, y) == TileConstants.LEVEL_COMPLETE_TILE) {
                    hasExit = true;
                    break;
                }
            }
            if (hasExit) break;
        }
        
        if (!hasExit) {
            issues.add(new ValidationIssue(Severity.ERROR, "Exit",
                "No exit tile found (tile ID " + TileConstants.LEVEL_COMPLETE_TILE + ")"));
        }
    }
    
    private static void validateSpiders(LevelData data, List<ValidationIssue> issues) {
        for (int i = 0; i < data.getSpiders().size(); i++) {
            SpiderData spider = data.getSpiders().get(i);
            List<PointData> waypoints = spider.waypoints;
            
            // Empty or single-point path
            if (waypoints == null || waypoints.isEmpty()) {
                issues.add(new ValidationIssue(Severity.ERROR, "Spider",
                    "Spider has no waypoints", i, "spider"));
                continue;
            }
            
            if (waypoints.size() < 2) {
                issues.add(new ValidationIssue(Severity.WARNING, "Spider",
                    "Spider has only 1 waypoint (will not patrol)", i, "spider"));
            }
            
            // Check each waypoint
            for (int j = 0; j < waypoints.size(); j++) {
                PointData wp = waypoints.get(j);
                
                // Out of bounds
                if (wp.x < 0 || wp.x >= data.getWidth() || wp.y < 0 || wp.y >= data.getHeight()) {
                    issues.add(new ValidationIssue(Severity.ERROR, "Spider",
                        "Waypoint " + (j+1) + " is outside map bounds", i, "spider", wp.x, wp.y));
                }
                // On solid tile
                else if (TileConstants.isSolidTile(data.getTile(wp.x, wp.y))) {
                    issues.add(new ValidationIssue(Severity.WARNING, "Spider",
                        "Waypoint " + (j+1) + " is on solid tile", i, "spider", wp.x, wp.y));
                }
            }
        }
    }
    
    private static void validateSnails(LevelData data, List<ValidationIssue> issues) {
        for (int i = 0; i < data.getSnails().size(); i++) {
            SnailData snail = data.getSnails().get(i);
            PointData p = snail.position;
            int tileX = p.x / TILE_SIZE;
            int tileY = p.y / TILE_SIZE;
            
            // Out of bounds
            if (tileX < 0 || tileX >= data.getWidth() || tileY < 0 || tileY >= data.getHeight()) {
                issues.add(new ValidationIssue(Severity.ERROR, "Snail",
                    "Snail position is outside map bounds", i, "snail", tileX, tileY));
            }
            
            // Empty dialogue
            if (snail.dialogue == null || snail.dialogue.isEmpty()) {
                issues.add(new ValidationIssue(Severity.WARNING, "Snail",
                    "Snail has no dialogue", i, "snail"));
            }
        }
    }
    
    private static void validateFood(LevelData data, List<ValidationIssue> issues) {
        boolean hasEnergySeed = false;
        
        for (int i = 0; i < data.getFood().size(); i++) {
            FoodData food = data.getFood().get(i);
            PointData p = food.position;
            
            // Out of bounds
            if (p.x < 0 || p.x >= data.getWidth() || p.y < 0 || p.y >= data.getHeight()) {
                issues.add(new ValidationIssue(Severity.ERROR, "Food",
                    "Food is outside map bounds", i, "food", p.x, p.y));
            }
            // On solid tile
            else if (TileConstants.isSolidTile(data.getTile(p.x, p.y))) {
                issues.add(new ValidationIssue(Severity.WARNING, "Food",
                    "Food is on solid tile (unreachable)", i, "food", p.x, p.y));
            }
            
            if ("ENERGY_SEED".equals(food.type)) {
                hasEnergySeed = true;
            }
        }
        
        // Energy seeds without mechanic enabled
        if (hasEnergySeed && !data.getMechanics().speedBoostFoodEnabled) {
            issues.add(new ValidationIssue(Severity.WARNING, "Food",
                "Energy seeds placed but speedBoostFood mechanic is disabled"));
        }
    }
    
    private static void validateTripwires(LevelData data, List<ValidationIssue> issues) {
        if (!data.getTripwires().isEmpty() && !data.getMechanics().tripWiresEnabled) {
            issues.add(new ValidationIssue(Severity.WARNING, "Tripwire",
                "Tripwires placed but tripWires mechanic is disabled"));
        }
        
        for (int i = 0; i < data.getTripwires().size(); i++) {
            PointData p = data.getTripwires().get(i);
            int tileX = p.x / TILE_SIZE;
            int tileY = p.y / TILE_SIZE;
            
            if (tileX < 0 || tileX >= data.getWidth() || tileY < 0 || tileY >= data.getHeight()) {
                issues.add(new ValidationIssue(Severity.ERROR, "Tripwire",
                    "Tripwire is outside map bounds", i, "tripwire", tileX, tileY));
            }
        }
    }
    
    private static void validateMapQuality(LevelData data, List<ValidationIssue> issues) {
        int solidCount = 0;
        int totalTiles = data.getWidth() * data.getHeight();
        
        for (int y = 0; y < data.getHeight(); y++) {
            for (int x = 0; x < data.getWidth(); x++) {
                if (TileConstants.isSolidTile(data.getTile(x, y))) {
                    solidCount++;
                }
            }
        }
        
        double solidRatio = (double) solidCount / totalTiles;
        
        if (solidRatio > 0.7) {
            issues.add(new ValidationIssue(Severity.INFO, "Quality",
                String.format("High solid tile ratio (%.0f%%) - may feel cramped", solidRatio * 100)));
        }
        
        if (solidRatio < 0.1) {
            issues.add(new ValidationIssue(Severity.INFO, "Quality",
                String.format("Low solid tile ratio (%.0f%%) - very open level", solidRatio * 100)));
        }
        
        // Check for very small map
        if (data.getWidth() < 10 || data.getHeight() < 10) {
            issues.add(new ValidationIssue(Severity.INFO, "Quality",
                "Map is very small (" + data.getWidth() + "x" + data.getHeight() + ")"));
        }
        
        // Check for no walls on edges
        boolean hasBorderWalls = true;
        for (int x = 0; x < data.getWidth(); x++) {
            if (!TileConstants.isSolidTile(data.getTile(x, 0)) || 
                !TileConstants.isSolidTile(data.getTile(x, data.getHeight() - 1))) {
                hasBorderWalls = false;
                break;
            }
        }
        for (int y = 0; y < data.getHeight() && hasBorderWalls; y++) {
            if (!TileConstants.isSolidTile(data.getTile(0, y)) || 
                !TileConstants.isSolidTile(data.getTile(data.getWidth() - 1, y))) {
                hasBorderWalls = false;
                break;
            }
        }
        
        if (!hasBorderWalls) {
            issues.add(new ValidationIssue(Severity.WARNING, "Quality",
                "Map edges are not fully walled - player may walk off map"));
        }
    }
    
    /**
     * Get count of issues by severity.
     */
    public static int countBySeverity(List<ValidationIssue> issues, Severity severity) {
        return (int) issues.stream().filter(i -> i.severity == severity).count();
    }
    
    /**
     * Check if level has critical errors.
     */
    public static boolean hasErrors(List<ValidationIssue> issues) {
        return issues.stream().anyMatch(i -> i.severity == Severity.ERROR);
    }
}
