package com.buglife.world;

import javax.imageio.ImageIO;
import java.io.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.awt.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.buglife.assets.AssetManager;

public class World {
    private static final Logger logger = LoggerFactory.getLogger(World.class);
    public static final int TILE_SIZE = 64; // The size of each tile in pixels

    private static Tile[] tileTypes; // An array to hold our different tile types (floor, wall, etc.)
    private int[][] mapData; // The 2D array that is our level design

    public int getMapWidth() {
        return mapData[0].length;
    }

    public int getMapHeight() {
        return mapData.length;
    }

    // Add this method to your World.java class
    public int getTileIdAt(int mapCol, int mapRow) {
        // Check if the coordinate is out of bounds
        if (mapRow < 0 || mapRow >= mapData.length || mapCol < 0 || mapCol >= mapData[0].length) {
            return -1; // Return an invalid ID for out-of-bounds
        }
        // Return the tile ID from our map data
        return mapData[mapRow][mapCol];
    }
    public boolean checkCollision(int x, int y, int width, int height) {
        // Check all four corners of the bounding box
        int left = x;
        int right = x + width - 1;
        int top = y;
        int bottom = y + height - 1;

        // Check if any corner hits a solid tile
        if (isTileSolid(left, top)) return true;      // Top-left
        if (isTileSolid(right, top)) return true;     // Top-right
        if (isTileSolid(left, bottom)) return true;   // Bottom-left
        if (isTileSolid(right, bottom)) return true;  // Bottom-right

        // Also check the center for more accuracy
        int centerX = x + width / 2;
        int centerY = y + height / 2;
        if (isTileSolid(centerX, centerY)) return true;

        return false; // No collision detected
    }

    // Alternative overloaded version that accepts a center point and size
    public boolean checkCollision(double centerX, double centerY, int width, int height) {
        return checkCollision((int)(centerX - width/2.0), (int)(centerY - height/2.0), width, height);
    }

    // Add this method to your World.java class
    public boolean isTileSolid(int worldX, int worldY) {
        // Convert world pixel coordinates to map grid coordinates
        if (worldX < 0 || worldY < 0) return true;

        int mapCol = worldX / TILE_SIZE;
        int mapRow = worldY / TILE_SIZE;

        // First, check if the coordinate is even on the map
        

        // Get the ID of the tile at that grid position
        int tileID = mapData[mapRow][mapCol];

        // Return whether that tile type is solid or not
        return tileTypes[tileID].solid;
    }

    public World() {
        
        if(tileTypes == null){
            loadTileTypes();
        }
        
        loadMapFromFile("/res/maps/level1.txt");
    }

    private void loadMapFromFile(String filePath) {
        List<List<Integer>> mapRows = new ArrayList<>();
        try {
            InputStream is = getClass().getResourceAsStream(filePath);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue; // Skip empty lines

                List<Integer> row = new ArrayList<>();
                String[] numbers = line.trim().split("\\s+"); // Split by one or more spaces
                for (String num : numbers) {
                    row.add(Integer.parseInt(num));
                }
                mapRows.add(row);
            }
            reader.close();

        } catch (Exception e) {
            logger.error("Failed to load map file: {}", filePath, e);
            return;
        }

        // Convert our flexible list into a rigid 2D array for performance
        int height = mapRows.size();
        int width = mapRows.get(0).size();
        this.mapData = new int[height][width];
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                this.mapData[row][col] = mapRows.get(row).get(col);
            }
        }
    } 

    private void loadTileTypes() {
        tileTypes = new Tile[50]; // We have 2 types of tiles right now
        try {
            // Tile 0: The Floor
            BufferedImage floorImage = AssetManager.getInstance().loadImage("/res/sprites/tiles/floor_1.png");
            tileTypes[0] = new Tile(floorImage, false); // false = not solid

            // Tile 1: The Wall
            BufferedImage wallImage = AssetManager.getInstance().loadImage("/res/sprites/tiles/wall_5.png");
            tileTypes[1] = new Tile(wallImage, true);
            
            BufferedImage wallImage1 = AssetManager.getInstance().loadImage("/res/sprites/tiles/wall.png");
            tileTypes[2] = new Tile(wallImage1, true);

            BufferedImage stickyImage = AssetManager.getInstance().loadImage("/res/sprites/tiles/sticky_floor.png");
            tileTypes[3] = new Tile(stickyImage, false);
            
            BufferedImage btile = AssetManager.getInstance().loadImage("/res/sprites/tiles/broken_tile.png");
            tileTypes[4] = new Tile(btile, true);

            BufferedImage shadowImage = AssetManager.getInstance().loadImage("/res/sprites/tiles/shadow_tile.png");
            tileTypes[5] = new Tile(shadowImage, false);
            

            //stain floor
             BufferedImage mtile1 = AssetManager.getInstance().loadImage("/res/sprites/tiles/stain_1.png");
            tileTypes[6] = new Tile(mtile1, false);
             BufferedImage mtile2 = AssetManager.getInstance().loadImage("/res/sprites/tiles/stain_2.png");
            tileTypes[7] = new Tile(mtile2, false); 
             BufferedImage mtile3 = AssetManager.getInstance().loadImage("/res/sprites/tiles/stain_3.png");
            tileTypes[8] = new Tile(mtile3, false); 
             BufferedImage mtile4 = AssetManager.getInstance().loadImage("/res/sprites/tiles/stain_4.png");
            tileTypes[9] = new Tile(mtile4, false);

            //chimney intro tiles
            BufferedImage itile1 = AssetManager.getInstance().loadImage("/res/sprites/tiles/introtile1.png");
            tileTypes[41] = new Tile(itile1, false);
            BufferedImage itile2 = AssetManager.getInstance().loadImage("/res/sprites/tiles/introtile2.png");
            tileTypes[42] = new Tile(itile2, false);
            BufferedImage itile3 = AssetManager.getInstance().loadImage("/res/sprites/tiles/introtile3.png");
            tileTypes[43] = new Tile(itile3, false);
            BufferedImage itile4 = AssetManager.getInstance().loadImage("/res/sprites/tiles/introtile4.png");
            tileTypes[44] = new Tile(itile4, true); 
            BufferedImage itile5 = AssetManager.getInstance().loadImage("/res/sprites/tiles/introtile5.png");
            tileTypes[45] = new Tile(itile5, true); 
            BufferedImage itile6 = AssetManager.getInstance().loadImage("/res/sprites/tiles/introtile6.png");
            tileTypes[46] = new Tile(itile6, true);     

            //plank tiles
             BufferedImage ptile1 = AssetManager.getInstance().loadImage("/res/sprites/tiles/plank1.png");
            tileTypes[31] = new Tile(ptile1, false);
             BufferedImage ptile2 = AssetManager.getInstance().loadImage("/res/sprites/tiles/plank2.png");
            tileTypes[32] = new Tile(ptile2, false); 
             BufferedImage ptile3 = AssetManager.getInstance().loadImage("/res/sprites/tiles/plank3.png");
            tileTypes[33] = new Tile(ptile3, false); 
             BufferedImage ptile4 = AssetManager.getInstance().loadImage("/res/sprites/tiles/plank4.png");
            tileTypes[34] = new Tile(ptile4, false);

            //ladder tiles
             BufferedImage ltile1 = AssetManager.getInstance().loadImage("/res/sprites/tiles/l1.png");
            tileTypes[35] = new Tile(ltile1, true);
             BufferedImage ltile2 = AssetManager.getInstance().loadImage("/res/sprites/tiles/l2.png");
            tileTypes[36] = new Tile(ltile2, true); 
             BufferedImage ltile3 = AssetManager.getInstance().loadImage("/res/sprites/tiles/l3.png");
            tileTypes[37] = new Tile(ltile3, false); 
             BufferedImage ltile4 = AssetManager.getInstance().loadImage("/res/sprites/tiles/l4.png");
            tileTypes[38] = new Tile(ltile4, true);

            //sack prop tiles
             BufferedImage stile1 = AssetManager.getInstance().loadImage("/res/sprites/tiles/sack_w1.png");
            tileTypes[11] = new Tile(stile1, true);
             BufferedImage stile2 = AssetManager.getInstance().loadImage("/res/sprites/tiles/sack_w2.png");
            tileTypes[12] = new Tile(stile2, true); 
             BufferedImage stile3 = AssetManager.getInstance().loadImage("/res/sprites/tiles/sack_w3.png");
            tileTypes[13] = new Tile(stile3, false); 
             BufferedImage stile4 = AssetManager.getInstance().loadImage("/res/sprites/tiles/sack_w4.png");
            tileTypes[14] = new Tile(stile4, true);


        } catch (Exception e) {
            logger.error("Failed to load tile images", e);
        }
    }
    // Add this method to World.java

    public List<Point> findSpiderPath() {
        List<Point> path = new ArrayList<>();
        for (int row = 0; row < mapData.length; row++) {
            for (int col = 0; col < mapData[row].length; col++) {
                if (mapData[row][col] == 2) {
                    // We add the TILE grid coordinates, not pixels
                    path.add(new Point(col, row));
                }
            }
        }
        // This simple version just adds them in reading order.
        // More complex versions could sort them to make a clean path.
        return path;
    }

 

    // In World.java

    public void render(Graphics g, int cameraX, int cameraY, int screenWidth, int screenHeight) {
        // 1. Calculate the range of tiles that are visible on screen.
        int startCol = cameraX / TILE_SIZE;
        int endCol = (cameraX + screenWidth) / TILE_SIZE + 1; // +1 to prevent gaps at the edge
        int startRow = cameraY / TILE_SIZE;
        int endRow = (cameraY + screenHeight) / TILE_SIZE + 1;

        // 2. Make sure we don't try to draw tiles that don't exist off the map edge.
        startCol = Math.max(0, startCol);
        endCol = Math.min(getMapWidth(), endCol);
        startRow = Math.max(0, startRow);
        endRow = Math.min(getMapHeight(), endRow);

        // 3. Now, loop ONLY through the visible tiles!
        for (int row = startRow; row < endRow; row++) {
            for (int col = startCol; col < endCol; col++) {
                int tileID = mapData[row][col];
                Tile tileToDraw = tileTypes[tileID];

                if (tileToDraw != null && tileToDraw.image != null) {
                    // Calculate where to draw the tile on the screen
                    int tileX = col * TILE_SIZE - cameraX;
                    int tileY = row * TILE_SIZE - cameraY;
                    g.drawImage(tileToDraw.image, tileX, tileY, TILE_SIZE, TILE_SIZE, null);
                }
            }
        }
    }
}