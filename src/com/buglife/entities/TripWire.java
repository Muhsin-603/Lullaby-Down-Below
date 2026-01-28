package com.buglife.entities;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TripWire {
    private static final Logger logger = LoggerFactory.getLogger(TripWire.class);
    private int x, y;
    private int width = 32, height = 32;
    private boolean broken = false;
    
    private int soundRadius = 500; // Default radius, adjustable via setter

    public TripWire(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void draw(Graphics2D g) {
        if (broken) return;

        // Draw the "X with a Circle inside"
        g.setColor(new Color(200, 200, 200, 180)); 
        g.setStroke(new BasicStroke(2)); // Make lines a bit thicker

        int cx = x + width / 2;
        int cy = y + height / 2;
        int size = 20; // Size of the symbol

        // The Circle
        g.drawOval(cx - size/2, cy - size/2, size, size);

        // The X (extending slightly beyond the circle)
        g.drawLine(cx - size/2 - 5, cy - size/2 - 5, cx + size/2 + 5, cy + size/2 + 5);
        g.drawLine(cx + size/2 + 5, cy - size/2 - 5, cx - size/2 - 5, cy + size/2 + 5);
        
        // Reset stroke
        g.setStroke(new BasicStroke(1));
    }

    public boolean checkCollision(Player p) {
        if (broken) return false;

        Rectangle playerBounds = new Rectangle((int)p.getX(), (int)p.getY(), 32, 32);
        Rectangle wireBounds = new Rectangle(x, y, width, height);

        if (playerBounds.intersects(wireBounds)) {
            broken = true;
            return true;
        }
        return false;
    }
    
    public int getX() { return x; }
    public int getY() { return y; }
    public boolean isBroken() { return broken; }
    
    public int getSoundRadius() { return soundRadius; }
    public void setSoundRadius(int radius) { this.soundRadius = radius; }
}