package com.buglife.engine.editor.tools;

import com.buglife.engine.editor.data.LevelData;

import java.util.*;

/**
 * Tile editing tools: brush, eraser, rectangle, fill, line.
 * All operations return undo actions.
 */
public class TileTools {
    
    private int selectedTileId = 0;
    private ToolMode mode = ToolMode.BRUSH;
    private int brushSize = 1;
    
    public enum ToolMode {
        BRUSH("Brush", "Paint tiles with selected tile"),
        ERASER("Eraser", "Erase to floor tile (0)"),
        RECTANGLE("Rectangle", "Fill rectangular area"),
        FILL("Fill", "Flood fill contiguous area"),
        LINE("Line", "Draw line between two points"),
        EYEDROPPER("Eyedropper", "Pick tile from map");
        
        public final String name;
        public final String description;
        
        ToolMode(String name, String description) {
            this.name = name;
            this.description = description;
        }
    }
    
    // ========== TOOL STATE ==========
    
    public ToolMode getMode() { return mode; }
    public void setMode(ToolMode mode) { this.mode = mode; }
    
    public int getSelectedTileId() { return selectedTileId; }
    public void setSelectedTileId(int id) { this.selectedTileId = id; }
    
    public int getBrushSize() { return brushSize; }
    public void setBrushSize(int size) { this.brushSize = Math.max(1, Math.min(10, size)); }
    
    // ========== TOOL OPERATIONS ==========
    
    /**
     * Apply brush at position.
     */
    public UndoManager.EditAction brush(LevelData data, int x, int y) {
        int tileId = mode == ToolMode.ERASER ? 0 : selectedTileId;
        return brush(data, x, y, tileId);
    }

    /**
     * Apply brush at position with specific tile ID.
     */
    public UndoManager.EditAction brush(LevelData data, int x, int y, int tileId) {
        if (brushSize == 1) {
            int old = data.getTile(x, y);
            if (old == tileId) return null;
            data.setTile(x, y, tileId);
            return new UndoManager.TileAction(x, y, old, tileId);
        } else {
            return brushArea(data, x, y, tileId);
        }
    }
    
    private UndoManager.EditAction brushArea(LevelData data, int cx, int cy, int tileId) {
        List<int[]> changes = new ArrayList<>();
        int radius = brushSize / 2;
        
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                int x = cx + dx;
                int y = cy + dy;
                
                if (x < 0 || x >= data.getWidth() || y < 0 || y >= data.getHeight()) {
                    continue;
                }
                
                // Circle brush shape
                if (dx * dx + dy * dy <= radius * radius) {
                    int old = data.getTile(x, y);
                    if (old != tileId) {
                        data.setTile(x, y, tileId);
                        changes.add(new int[]{x, y, old, tileId});
                    }
                }
            }
        }
        
        if (changes.isEmpty()) return null;
        return new UndoManager.MultiTileAction(changes, "Brush paint");
    }
    
    /**
     * Fill rectangle between two corners.
     */
    public UndoManager.EditAction rectangle(LevelData data, int x1, int y1, int x2, int y2) {
        int tileId = mode == ToolMode.ERASER ? 0 : selectedTileId;
        return rectangle(data, x1, y1, x2, y2, tileId);
    }

    /**
     * Fill rectangle between two corners with specific tile ID.
     */
    public UndoManager.EditAction rectangle(LevelData data, int x1, int y1, int x2, int y2, int tileId) {
        
        int minX = Math.max(0, Math.min(x1, x2));
        int maxX = Math.min(data.getWidth() - 1, Math.max(x1, x2));
        int minY = Math.max(0, Math.min(y1, y2));
        int maxY = Math.min(data.getHeight() - 1, Math.max(y1, y2));
        
        List<int[]> changes = new ArrayList<>();
        
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                int old = data.getTile(x, y);
                if (old != tileId) {
                    data.setTile(x, y, tileId);
                    changes.add(new int[]{x, y, old, tileId});
                }
            }
        }
        
        if (changes.isEmpty()) return null;
        return new UndoManager.MultiTileAction(changes, 
            "Rectangle fill (" + (maxX - minX + 1) + "x" + (maxY - minY + 1) + ")");
    }
    
    /**
     * Flood fill from starting position.
     */
    public UndoManager.EditAction fill(LevelData data, int startX, int startY) {
        int fillTile = mode == ToolMode.ERASER ? 0 : selectedTileId;
        return fill(data, startX, startY, fillTile);
    }

    /**
     * Flood fill from starting position with specific tile ID.
     */
    public UndoManager.EditAction fill(LevelData data, int startX, int startY, int fillTile) {
        int targetTile = data.getTile(startX, startY);
        
        if (targetTile == fillTile || targetTile < 0) {
            return null;
        }
        
        List<int[]> changes = new ArrayList<>();
        Queue<int[]> queue = new LinkedList<>();
        Set<Long> visited = new HashSet<>();
        
        queue.add(new int[]{startX, startY});
        visited.add(packCoord(startX, startY));
        
        int maxIterations = data.getWidth() * data.getHeight();
        int iterations = 0;
        
        while (!queue.isEmpty() && iterations++ < maxIterations) {
            int[] pos = queue.poll();
            int x = pos[0];
            int y = pos[1];
            
            int old = data.getTile(x, y);
            if (old != targetTile) continue;
            
            data.setTile(x, y, fillTile);
            changes.add(new int[]{x, y, old, fillTile});
            
            // Check 4 neighbors
            int[][] neighbors = {{x-1, y}, {x+1, y}, {x, y-1}, {x, y+1}};
            for (int[] n : neighbors) {
                int nx = n[0], ny = n[1];
                if (nx < 0 || nx >= data.getWidth() || ny < 0 || ny >= data.getHeight()) {
                    continue;
                }
                
                long key = packCoord(nx, ny);
                if (!visited.contains(key) && data.getTile(nx, ny) == targetTile) {
                    visited.add(key);
                    queue.add(new int[]{nx, ny});
                }
            }
        }
        
        if (changes.isEmpty()) return null;
        return new UndoManager.MultiTileAction(changes, "Fill (" + changes.size() + " tiles)");
    }
    private long packCoord(int x, int y) {
        return (((long)x) << 32) | (y & 0xffffffffL);
    }
    
    /**
     * Draw line between two points using Bresenham's algorithm.
     */
    public UndoManager.EditAction line(LevelData data, int x1, int y1, int x2, int y2) {
        int tileId = mode == ToolMode.ERASER ? 0 : selectedTileId;
        return line(data, x1, y1, x2, y2, tileId);
    }

    /**
     * Draw line between two points with specific tile ID.
     */
    public UndoManager.EditAction line(LevelData data, int x1, int y1, int x2, int y2, int tileId) {
        List<int[]> changes = new ArrayList<>();
        
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int sx = x1 < x2 ? 1 : -1;
        int sy = y1 < y2 ? 1 : -1;
        int err = dx - dy;
        
        int x = x1, y = y1;
        
        while (true) {
            if (x >= 0 && x < data.getWidth() && y >= 0 && y < data.getHeight()) {
                int old = data.getTile(x, y);
                if (old != tileId) {
                    data.setTile(x, y, tileId);
                    changes.add(new int[]{x, y, old, tileId});
                }
            }
            
            if (x == x2 && y == y2) break;
            
            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x += sx;
            }
            if (e2 < dx) {
                err += dx;
                y += sy;
            }
        }
        
        if (changes.isEmpty()) return null;
        return new UndoManager.MultiTileAction(changes, "Line draw");
    }
    
    /**
     * Pick tile ID from map position.
     */
    public int eyedropper(LevelData data, int x, int y) {
        int tile = data.getTile(x, y);
        if (tile >= 0) {
            selectedTileId = tile;
        }
        return tile;
    }
    
    /**
     * Get line preview points (for visual feedback during drag).
     */
    public java.util.List<java.awt.Point> linePreview(int x1, int y1, int x2, int y2) {
        java.util.List<java.awt.Point> points = new ArrayList<>();
        
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int sx = x1 < x2 ? 1 : -1;
        int sy = y1 < y2 ? 1 : -1;
        int err = dx - dy;
        
        int x = x1, y = y1;
        
        while (true) {
            points.add(new java.awt.Point(x, y));
            if (x == x2 && y == y2) break;
            
            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x += sx;
            }
            if (e2 < dx) {
                err += dx;
                y += sy;
            }
        }
        return points;
    }
}

