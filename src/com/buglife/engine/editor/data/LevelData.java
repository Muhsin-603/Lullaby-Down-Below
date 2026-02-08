package com.buglife.engine.editor.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

/**
 * Complete level data model for the editor.
 * Persisted as JSON for easy editing and version control.
 * 
 * This is the single source of truth for level design.
 * The game can load this directly or it can be converted to LevelConfig classes.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class LevelData {
    
    // Metadata
    @JsonProperty("version")
    private int version = 1;
    
    @JsonProperty("levelName")
    private String levelName = "untitled";
    
    @JsonProperty("author")
    private String author = "";
    
    @JsonProperty("description")
    private String description = "";
    
    // Map dimensions
    @JsonProperty("width")
    private int width = 20;
    
    @JsonProperty("height")
    private int height = 20;
    
    // Tile data (flattened 2D array: row-major order)
    @JsonProperty("tiles")
    private int[] tiles;
    
    // Entity spawns
    @JsonProperty("playerSpawn")
    private PointData playerSpawn = new PointData(64, 64);
    
    @JsonProperty("toySpawn")
    private PointData toySpawn;
    
    @JsonProperty("snails")
    private List<SnailData> snails = new ArrayList<>();
    
    @JsonProperty("spiders")
    private List<SpiderData> spiders = new ArrayList<>();
    
    @JsonProperty("food")
    private List<FoodData> food = new ArrayList<>();
    
    @JsonProperty("tripwires")
    private List<PointData> tripwires = new ArrayList<>();
    
    // Mechanics config
    @JsonProperty("mechanics")
    private MechanicsData mechanics = new MechanicsData();
    
    // ========== CONSTRUCTORS ==========
    
    public LevelData() {
        initializeTiles();
    }
    
    public LevelData(int width, int height) {
        this.width = width;
        this.height = height;
        initializeTiles();
    }

    public LevelData(String name, int width, int height) {
        this.levelName = name;
        this.width = width;
        this.height = height;
        initializeTiles();
    }

    // Alias for editor compatibility
    public String getName() { return levelName; }
    public void setName(String name) { this.levelName = name; }

    /**
     * Resize level, preserving existing tile data where possible.
     */
    public void resize(int newWidth, int newHeight) {
        int[] oldTiles = tiles;
        int oldWidth = width;
        int oldHeight = height;
        
        this.width = newWidth;
        this.height = newHeight;
        this.tiles = new int[newWidth * newHeight];
        
        // Copy existing tiles
        for (int y = 0; y < Math.min(oldHeight, newHeight); y++) {
            for (int x = 0; x < Math.min(oldWidth, newWidth); x++) {
                tiles[y * newWidth + x] = oldTiles[y * oldWidth + x];
            }
        }
    }

    private void initializeTiles() {
        tiles = new int[width * height];
        // Fill with floor tiles (0)
        for (int i = 0; i < tiles.length; i++) {
            tiles[i] = 0;
        }
        // Add border walls (1)
        for (int x = 0; x < width; x++) {
            setTile(x, 0, 1);
            setTile(x, height - 1, 1);
        }
        for (int y = 0; y < height; y++) {
            setTile(0, y, 1);
            setTile(width - 1, y, 1);
        }
    }
    
    // ========== TILE ACCESS ==========
    
    public int getTile(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            return -1;
        }
        return tiles[y * width + x];
    }
    
    public void setTile(int x, int y, int tileId) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            return;
        }
        tiles[y * width + x] = tileId;
    }
    
    public int[][] toTileArray() {
        int[][] result = new int[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                result[y][x] = getTile(x, y);
            }
        }
        return result;
    }
    
    public void fromTileArray(int[][] data) {
        this.height = data.length;
        this.width = data.length > 0 ? data[0].length : 0;
        this.tiles = new int[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                setTile(x, y, data[y][x]);
            }
        }
    }
    
    // ========== GETTERS/SETTERS ==========
    
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
    
    public String getLevelName() { return levelName; }
    public void setLevelName(String levelName) { this.levelName = levelName; }
    
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public int getWidth() { return width; }
    public void setWidth(int width) { this.width = width; }
    
    public int getHeight() { return height; }
    public void setHeight(int height) { this.height = height; }
    
    public int[] getTiles() { return tiles; }
    public void setTiles(int[] tiles) { this.tiles = tiles; }
    
    public PointData getPlayerSpawn() { return playerSpawn; }
    public void setPlayerSpawn(PointData playerSpawn) { this.playerSpawn = playerSpawn; }
    
    public PointData getToySpawn() { return toySpawn; }
    public void setToySpawn(PointData toySpawn) { this.toySpawn = toySpawn; }
    
    public List<SnailData> getSnails() { return snails; }
    public void setSnails(List<SnailData> snails) { this.snails = snails; }
    
    public List<SpiderData> getSpiders() { return spiders; }
    public void setSpiders(List<SpiderData> spiders) { this.spiders = spiders; }
    
    public List<FoodData> getFood() { return food; }
    public void setFood(List<FoodData> food) { this.food = food; }
    
    public List<PointData> getTripwires() { return tripwires; }
    public void setTripwires(List<PointData> tripwires) { this.tripwires = tripwires; }
    
    public MechanicsData getMechanics() { return mechanics; }
    public void setMechanics(MechanicsData mechanics) { this.mechanics = mechanics; }
    
    // ========== NESTED DATA CLASSES ==========
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PointData {
        @JsonProperty("x") public int x;
        @JsonProperty("y") public int y;
        
        public PointData() {}
        public PointData(int x, int y) { this.x = x; this.y = y; }
        
        public java.awt.Point toAwtPoint() { return new java.awt.Point(x, y); }
        public static PointData fromAwtPoint(java.awt.Point p) { return new PointData(p.x, p.y); }
    }
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SnailData {
        @JsonProperty("position") public PointData position = new PointData();
        @JsonProperty("dialogue") public List<String> dialogue = new ArrayList<>();
        @JsonProperty("requiresInteraction") public boolean requiresInteraction = true;
        @JsonProperty("description") public String description = "";
        
        public SnailData() {}
        public SnailData(int x, int y) { this.position = new PointData(x, y); }
    }
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SpiderData {
        @JsonProperty("waypoints") public List<PointData> waypoints = new ArrayList<>();
        @JsonProperty("description") public String description = "";
        
        public SpiderData() {}
        
        public void addWaypoint(int tileX, int tileY) {
            waypoints.add(new PointData(tileX, tileY));
        }
    }
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FoodData {
        @JsonProperty("position") public PointData position = new PointData();
        @JsonProperty("type") public String type = "BERRY"; // BERRY or ENERGY_SEED
        @JsonProperty("description") public String description = "";
        
        public FoodData() {}
        public FoodData(int tileX, int tileY, String type) {
            this.position = new PointData(tileX, tileY);
            this.type = type;
        }
    }
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MechanicsData {
        @JsonProperty("dashEnabled") public boolean dashEnabled = false;
        @JsonProperty("toyEnabled") public boolean toyEnabled = false;
        @JsonProperty("tripWiresEnabled") public boolean tripWiresEnabled = false;
        @JsonProperty("speedBoostFoodEnabled") public boolean speedBoostFoodEnabled = false;
    }
}
