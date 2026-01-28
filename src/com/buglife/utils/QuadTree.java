package com.buglife.utils;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

/**
 * QuadTree implementation for efficient spatial partitioning and collision detection.
 * Reduces collision checks from O(n²) to O(n log n) for large numbers of entities.
 * 
 * @param <T> Type of objects stored in the quadtree (must have bounds)
 */
public class QuadTree<T extends QuadTree.Bounded> {
    
    private static final int MAX_OBJECTS = 10;
    private static final int MAX_LEVELS = 5;
    
    private final int level;
    private final List<T> objects;
    private final Rectangle bounds;
    private QuadTree<T>[] nodes;
    
    /**
     * Interface for objects that can be stored in the QuadTree
     */
    public interface Bounded {
        Rectangle getBounds();
    }
    
    /**
     * Create a new QuadTree
     * @param level Current level in the tree (0 = root)
     * @param bounds Spatial bounds of this node
     */
    public QuadTree(int level, Rectangle bounds) {
        this.level = level;
        this.bounds = bounds;
        this.objects = new ArrayList<>();
    }
    
    /**
     * Clear the quadtree and all child nodes
     */
    public void clear() {
        objects.clear();
        
        if (nodes != null) {
            for (int i = 0; i < nodes.length; i++) {
                if (nodes[i] != null) {
                    nodes[i].clear();
                    nodes[i] = null;
                }
            }
            nodes = null;
        }
    }
    
    /**
     * Split the node into 4 quadrants
     */
    @SuppressWarnings("unchecked")
    private void split() {
        int subWidth = bounds.width / 2;
        int subHeight = bounds.height / 2;
        int x = bounds.x;
        int y = bounds.y;
        
        nodes = new QuadTree[4];
        nodes[0] = new QuadTree<>(level + 1, new Rectangle(x + subWidth, y, subWidth, subHeight)); // Top-right
        nodes[1] = new QuadTree<>(level + 1, new Rectangle(x, y, subWidth, subHeight));            // Top-left
        nodes[2] = new QuadTree<>(level + 1, new Rectangle(x, y + subHeight, subWidth, subHeight));// Bottom-left
        nodes[3] = new QuadTree<>(level + 1, new Rectangle(x + subWidth, y + subHeight, subWidth, subHeight)); // Bottom-right
    }
    
    /**
     * Determine which quadrant an object belongs to
     * @return Quadrant index (0-3) or -1 if doesn't fit in a single quadrant
     */
    private int getIndex(Rectangle rect) {
        int index = -1;
        
        double verticalMidpoint = bounds.x + (bounds.width / 2.0);
        double horizontalMidpoint = bounds.y + (bounds.height / 2.0);
        
        // Object fits in top half
        boolean topQuadrant = (rect.y < horizontalMidpoint && rect.y + rect.height < horizontalMidpoint);
        // Object fits in bottom half
        boolean bottomQuadrant = (rect.y > horizontalMidpoint);
        
        // Object fits in left half
        if (rect.x < verticalMidpoint && rect.x + rect.width < verticalMidpoint) {
            if (topQuadrant) {
                index = 1; // Top-left
            } else if (bottomQuadrant) {
                index = 2; // Bottom-left
            }
        }
        // Object fits in right half
        else if (rect.x > verticalMidpoint) {
            if (topQuadrant) {
                index = 0; // Top-right
            } else if (bottomQuadrant) {
                index = 3; // Bottom-right
            }
        }
        
        return index;
    }
    
    /**
     * Insert an object into the quadtree
     */
    public void insert(T obj) {
        if (nodes != null) {
            int index = getIndex(obj.getBounds());
            
            if (index != -1) {
                nodes[index].insert(obj);
                return;
            }
        }
        
        objects.add(obj);
        
        // Split if we exceed capacity and aren't at max depth
        if (objects.size() > MAX_OBJECTS && level < MAX_LEVELS) {
            if (nodes == null) {
                split();
            }
            
            // Redistribute objects to child nodes
            int i = 0;
            while (i < objects.size()) {
                int index = getIndex(objects.get(i).getBounds());
                if (index != -1) {
                    nodes[index].insert(objects.remove(i));
                } else {
                    i++;
                }
            }
        }
    }
    
    /**
     * Retrieve all objects that could collide with the given rectangle
     */
    public List<T> retrieve(List<T> returnObjects, Rectangle rect) {
        int index = getIndex(rect);
        if (index != -1 && nodes != null) {
            nodes[index].retrieve(returnObjects, rect);
        }
        
        returnObjects.addAll(objects);
        
        return returnObjects;
    }
    
    /**
     * Convenience method to retrieve potential collisions
     */
    public List<T> retrieve(Rectangle rect) {
        return retrieve(new ArrayList<>(), rect);
    }
    
    /**
     * Get total number of objects in the tree (including child nodes)
     */
    public int getTotalObjects() {
        int count = objects.size();
        
        if (nodes != null) {
            for (QuadTree<T> node : nodes) {
                if (node != null) {
                    count += node.getTotalObjects();
                }
            }
        }
        
        return count;
    }
    
    /**
     * Get the depth of the tree
     */
    public int getDepth() {
        if (nodes == null) {
            return level;
        }
        
        int maxChildDepth = level;
        for (QuadTree<T> node : nodes) {
            if (node != null) {
                maxChildDepth = Math.max(maxChildDepth, node.getDepth());
            }
        }
        
        return maxChildDepth;
    }
    
    /**
     * Check if the tree has been split
     */
    public boolean isSplit() {
        return nodes != null;
    }
    
    /**
     * Get the bounds of this quadtree node
     */
    public Rectangle getBounds() {
        return new Rectangle(bounds);
    }
}
