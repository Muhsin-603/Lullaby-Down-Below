package com.buglife.engine.editor.tools;

import com.buglife.engine.editor.data.LevelData;
import com.buglife.engine.editor.data.LevelData.*;

/**
 * Entity editing tools: placement, movement, deletion.
 * Handles player, toy, snails, spiders, food, tripwires.
 */
public class EntityTools {
    
    public enum EntityMode {
        SELECT("Select", "Select and move entities"),
        PLAYER_SPAWN("Player", "Set player spawn point"),
        TOY_SPAWN("Toy", "Set toy spawn point"),
        SNAIL("Snail", "Add/edit snail locations"),
        SPIDER("Spider", "Add/edit spider patrol paths"),
        FOOD_BERRY("Berry", "Add berry food"),
        FOOD_SEED("Energy Seed", "Add energy seed food"),
        TRIPWIRE("Tripwire", "Add tripwire");
        
        public final String name;
        public final String description;
        
        EntityMode(String name, String description) {
            this.name = name;
            this.description = description;
        }
    }
    
    private static final int TILE_SIZE = 64;
    
    private EntityMode mode = EntityMode.SELECT;
    private Object selectedEntity = null;
    private String selectedEntityType = null;
    private int selectedWaypointIndex = -1;
    
    // Spider path building state
    private SpiderData pendingSpider = null;
    
    // ========== MODE STATE ==========
    
    public EntityMode getMode() { return mode; }
    public void setMode(EntityMode mode) { 
        this.mode = mode;
        // Clear pending spider if switching modes
        if (mode != EntityMode.SPIDER) {
            pendingSpider = null;
        }
    }
    
    public Object getSelectedEntity() { return selectedEntity; }
    public String getSelectedEntityType() { return selectedEntityType; }
    
    public void clearSelection() {
        selectedEntity = null;
        selectedEntityType = null;
        selectedWaypointIndex = -1;
    }
    
    public SpiderData getPendingSpider() { return pendingSpider; }
    
    // ========== HIT TESTING ==========
    
    /**
     * Find entity at screen position (tile coordinates for spiders/food, pixel for others).
     */
    public HitResult hitTest(LevelData data, int tileX, int tileY, int pixelX, int pixelY) {
        // Check player spawn
        if (data.getPlayerSpawn() != null) {
            PointData p = data.getPlayerSpawn();
            if (isNearPixel(p, pixelX, pixelY)) {
                return new HitResult("player", data.getPlayerSpawn(), -1);
            }
        }
        
        // Check toy spawn
        if (data.getToySpawn() != null) {
            PointData p = data.getToySpawn();
            if (isNearPixel(p, pixelX, pixelY)) {
                return new HitResult("toy", data.getToySpawn(), -1);
            }
        }
        
        // Check snails
        for (int i = 0; i < data.getSnails().size(); i++) {
            SnailData snail = data.getSnails().get(i);
            if (isNearPixel(snail.position, pixelX, pixelY)) {
                return new HitResult("snail", snail, i);
            }
        }
        
        // Check spider waypoints
        for (int i = 0; i < data.getSpiders().size(); i++) {
            SpiderData spider = data.getSpiders().get(i);
            for (int j = 0; j < spider.waypoints.size(); j++) {
                PointData wp = spider.waypoints.get(j);
                if (isNearTile(wp, tileX, tileY)) {
                    return new HitResult("spider", spider, i, j);
                }
            }
        }
        
        // Check food
        for (int i = 0; i < data.getFood().size(); i++) {
            FoodData food = data.getFood().get(i);
            if (isNearTile(food.position, tileX, tileY)) {
                return new HitResult("food", food, i);
            }
        }
        
        // Check tripwires
        for (int i = 0; i < data.getTripwires().size(); i++) {
            PointData tw = data.getTripwires().get(i);
            if (isNearPixel(tw, pixelX, pixelY)) {
                return new HitResult("tripwire", tw, i);
            }
        }
        
        return null;
    }
    
    private boolean isNearPixel(PointData point, int pixelX, int pixelY) {
        int dx = point.x - pixelX;
        int dy = point.y - pixelY;
        return (dx * dx + dy * dy) <= (TILE_SIZE * TILE_SIZE);
    }
    
    private boolean isNearTile(PointData point, int tileX, int tileY) {
        return point.x == tileX && point.y == tileY;
    }
    
    public static class HitResult {
        public final String type;
        public final Object entity;
        public final int index;
        public final int waypointIndex;
        
        public HitResult(String type, Object entity, int index) {
            this(type, entity, index, -1);
        }
        
        public HitResult(String type, Object entity, int index, int waypointIndex) {
            this.type = type;
            this.entity = entity;
            this.index = index;
            this.waypointIndex = waypointIndex;
        }
    }
    
    // ========== ENTITY OPERATIONS ==========
    
    /**
     * Handle click in entity mode.
     */
    public UndoManager.EditAction handleClick(LevelData data, int tileX, int tileY, 
                                               int pixelX, int pixelY, UndoManager undoManager) {
        switch (mode) {
            case SELECT:
                HitResult hit = hitTest(data, tileX, tileY, pixelX, pixelY);
                if (hit != null) {
                    selectedEntity = hit.entity;
                    selectedEntityType = hit.type;
                    selectedWaypointIndex = hit.waypointIndex;
                }
                return null;
                
            case PLAYER_SPAWN:
                return setPlayerSpawn(data, pixelX, pixelY);
                
            case TOY_SPAWN:
                return setToySpawn(data, pixelX, pixelY);
                
            case SNAIL:
                return addSnail(data, pixelX, pixelY);
                
            case SPIDER:
                return handleSpiderClick(data, tileX, tileY);
                
            case FOOD_BERRY:
                return addFood(data, tileX, tileY, "BERRY");
                
            case FOOD_SEED:
                return addFood(data, tileX, tileY, "ENERGY_SEED");
                
            case TRIPWIRE:
                return addTripwire(data, pixelX, pixelY);
                
            default:
                return null;
        }
    }
    
    /**
     * Set player spawn point.
     */
    public UndoManager.EditAction setPlayerSpawn(LevelData data, int pixelX, int pixelY) {
        PointData oldSpawn = data.getPlayerSpawn();
        int oldX = oldSpawn != null ? oldSpawn.x : 0;
        int oldY = oldSpawn != null ? oldSpawn.y : 0;
        
        if (data.getPlayerSpawn() == null) {
            data.setPlayerSpawn(new PointData(pixelX, pixelY));
        } else {
            data.getPlayerSpawn().x = pixelX;
            data.getPlayerSpawn().y = pixelY;
        }
        
        return new UndoManager.EntityMoveAction(data.getPlayerSpawn(), 
            oldX, oldY, pixelX, pixelY, "player spawn");
    }
    
    /**
     * Set toy spawn point.
     */
    public UndoManager.EditAction setToySpawn(LevelData data, int pixelX, int pixelY) {
        if (data.getToySpawn() == null) {
            PointData newSpawn = new PointData(pixelX, pixelY);
            data.setToySpawn(newSpawn);
            // Can't use EntityAddAction for single spawn, use custom action
            return new UndoManager.EditAction() {
                @Override public void undo(LevelData d) { d.setToySpawn(null); }
                @Override public void redo(LevelData d) { d.setToySpawn(newSpawn); }
                @Override public String getDescription() { return "Add toy spawn"; }
            };
        } else {
            int oldX = data.getToySpawn().x;
            int oldY = data.getToySpawn().y;
            data.getToySpawn().x = pixelX;
            data.getToySpawn().y = pixelY;
            return new UndoManager.EntityMoveAction(data.getToySpawn(),
                oldX, oldY, pixelX, pixelY, "toy spawn");
        }
    }
    
    /**
     * Add snail at position.
     */
    public UndoManager.EditAction addSnail(LevelData data, int pixelX, int pixelY) {
        SnailData snail = new SnailData(pixelX, pixelY);
        snail.dialogue.add("Hello!");
        data.getSnails().add(snail);
        return new UndoManager.EntityAddAction<>(data.getSnails(), snail, "snail");
    }
    
    /**
     * Handle spider mode click (building patrol path).
     */
    private UndoManager.EditAction handleSpiderClick(LevelData data, int tileX, int tileY) {
        if (pendingSpider == null) {
            // Start new spider
            pendingSpider = new SpiderData();
            pendingSpider.addWaypoint(tileX, tileY);
            return null; // No undo yet, still building
        } else {
            // Add waypoint to pending spider
            pendingSpider.addWaypoint(tileX, tileY);
            return null; // Still building
        }
    }
    
    /**
     * Finish current spider patrol and add to level.
     */
    public UndoManager.EditAction finishSpider(LevelData data) {
        if (pendingSpider == null || pendingSpider.waypoints.size() < 2) {
            pendingSpider = null;
            return null;
        }
        
        SpiderData spider = pendingSpider;
        pendingSpider = null;
        data.getSpiders().add(spider);
        return new UndoManager.EntityAddAction<>(data.getSpiders(), spider, "spider");
    }
    
    /**
     * Cancel current spider building.
     */
    public void cancelSpider() {
        pendingSpider = null;
    }
    
    /**
     * Add food at tile position.
     */
    public UndoManager.EditAction addFood(LevelData data, int tileX, int tileY, String type) {
        FoodData food = new FoodData(tileX, tileY, type);
        data.getFood().add(food);
        return new UndoManager.EntityAddAction<>(data.getFood(), food, type.toLowerCase());
    }
    
    /**
     * Add tripwire at pixel position.
     */
    public UndoManager.EditAction addTripwire(LevelData data, int pixelX, int pixelY) {
        PointData tw = new PointData(pixelX, pixelY);
        data.getTripwires().add(tw);
        return new UndoManager.EntityAddAction<>(data.getTripwires(), tw, "tripwire");
    }
    
    /**
     * Delete selected entity.
     */
    public UndoManager.EditAction deleteSelected(LevelData data) {
        if (selectedEntity == null) return null;
        
        UndoManager.EditAction action = null;
        
        switch (selectedEntityType) {
            case "player":
                // Can't delete player spawn, just clear selection
                break;
                
            case "toy":
                PointData toySpawn = data.getToySpawn();
                data.setToySpawn(null);
                action = new UndoManager.EditAction() {
                    @Override public void undo(LevelData d) { d.setToySpawn(toySpawn); }
                    @Override public void redo(LevelData d) { d.setToySpawn(null); }
                    @Override public String getDescription() { return "Remove toy spawn"; }
                };
                break;
                
            case "snail":
                SnailData snail = (SnailData) selectedEntity;
                int snailIdx = data.getSnails().indexOf(snail);
                data.getSnails().remove(snail);
                action = new UndoManager.EntityRemoveAction<>(data.getSnails(), snail, snailIdx, "snail");
                break;
                
            case "spider":
                if (selectedWaypointIndex >= 0) {
                    // Delete single waypoint
                    SpiderData spider = (SpiderData) selectedEntity;
                    if (spider.waypoints.size() > 2) {
                        PointData wp = spider.waypoints.remove(selectedWaypointIndex);
                        final int wpIdx = selectedWaypointIndex;
                        action = new UndoManager.EditAction() {
                            @Override public void undo(LevelData d) { spider.waypoints.add(wpIdx, wp); }
                            @Override public void redo(LevelData d) { spider.waypoints.remove(wp); }
                            @Override public String getDescription() { return "Remove waypoint"; }
                        };
                    } else {
                        // Too few waypoints, delete entire spider
                        int spiderIdx = data.getSpiders().indexOf(spider);
                        data.getSpiders().remove(spider);
                        action = new UndoManager.EntityRemoveAction<>(data.getSpiders(), spider, spiderIdx, "spider");
                    }
                } else {
                    // Delete entire spider
                    SpiderData spider = (SpiderData) selectedEntity;
                    int spiderIdx = data.getSpiders().indexOf(spider);
                    data.getSpiders().remove(spider);
                    action = new UndoManager.EntityRemoveAction<>(data.getSpiders(), spider, spiderIdx, "spider");
                }
                break;
                
            case "food":
                FoodData food = (FoodData) selectedEntity;
                int foodIdx = data.getFood().indexOf(food);
                data.getFood().remove(food);
                action = new UndoManager.EntityRemoveAction<>(data.getFood(), food, foodIdx, "food");
                break;
                
            case "tripwire":
                PointData tw = (PointData) selectedEntity;
                int twIdx = data.getTripwires().indexOf(tw);
                data.getTripwires().remove(tw);
                action = new UndoManager.EntityRemoveAction<>(data.getTripwires(), tw, twIdx, "tripwire");
                break;
        }
        
        clearSelection();
        return action;
    }
    
    /**
     * Move selected entity to new position.
     */
    public UndoManager.EditAction moveSelected(LevelData data, int tileX, int tileY, 
                                                int pixelX, int pixelY) {
        if (selectedEntity == null) return null;
        
        switch (selectedEntityType) {
            case "player": {
                PointData p = data.getPlayerSpawn();
                int oldX = p.x, oldY = p.y;
                p.x = pixelX;
                p.y = pixelY;
                return new UndoManager.EntityMoveAction(p, oldX, oldY, pixelX, pixelY, "player");
            }
            
            case "toy": {
                PointData p = data.getToySpawn();
                int oldX = p.x, oldY = p.y;
                p.x = pixelX;
                p.y = pixelY;
                return new UndoManager.EntityMoveAction(p, oldX, oldY, pixelX, pixelY, "toy");
            }
            
            case "snail": {
                SnailData snail = (SnailData) selectedEntity;
                int oldX = snail.position.x, oldY = snail.position.y;
                snail.position.x = pixelX;
                snail.position.y = pixelY;
                return new UndoManager.EntityMoveAction(snail.position, oldX, oldY, pixelX, pixelY, "snail");
            }
            
            case "spider": {
                if (selectedWaypointIndex >= 0) {
                    SpiderData spider = (SpiderData) selectedEntity;
                    PointData wp = spider.waypoints.get(selectedWaypointIndex);
                    int oldX = wp.x, oldY = wp.y;
                    wp.x = tileX;
                    wp.y = tileY;
                    return new UndoManager.EntityMoveAction(wp, oldX, oldY, tileX, tileY, "waypoint");
                }
                break;
            }
            
            case "food": {
                FoodData food = (FoodData) selectedEntity;
                int oldX = food.position.x, oldY = food.position.y;
                food.position.x = tileX;
                food.position.y = tileY;
                return new UndoManager.EntityMoveAction(food.position, oldX, oldY, tileX, tileY, "food");
            }
            
            case "tripwire": {
                PointData tw = (PointData) selectedEntity;
                int oldX = tw.x, oldY = tw.y;
                tw.x = pixelX;
                tw.y = pixelY;
                return new UndoManager.EntityMoveAction(tw, oldX, oldY, pixelX, pixelY, "tripwire");
            }
        }
        
        return null;
    }
}
