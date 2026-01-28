package com.buglife.entities;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.AlphaComposite;

public class Food {
    private int x, y;
    private int size;
    
    public enum FoodType {
        BERRY(25, Color.YELLOW),
        // Changed from CYAN to GREEN as requested!
        ENERGY_SEED(15, Color.GREEN); 

        public final int hungerRestore;
        public final Color color;

        FoodType(int hunger, Color color) {
            this.hungerRestore = hunger;
            this.color = color;
        }
    }

    private FoodType type;

    public Food(int x, int y, int size, FoodType type) {
        this.x = x;
        this.y = y;
        this.size = size;
        this.type = type;
    }

    private boolean isBeingEaten = false;
    private int eatAnimationTick = 0;
    private final int EAT_ANIMATION_DURATION = 5;

    public void draw(Graphics g) {
        Color foodColor = type.color;

        if (!isBeingEaten) {
            g.setColor(foodColor);
            g.fillOval(x, y, size, size);
            
            // Glow effect
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
            g2d.setColor(Color.WHITE);
            g2d.fillOval(x - 2, y - 2, size + 4, size + 4);
            
            // Energy seeds get a little extra sparkle in the center
            if (type == FoodType.ENERGY_SEED) {
                g2d.setColor(new Color(200, 255, 200)); // Light green center
                g2d.fillOval(x + size/2 - 2, y + size/2 - 2, 4, 4);
            }
            g2d.dispose();

        } else {
            float alpha = 1.0f - (float)eatAnimationTick / EAT_ANIMATION_DURATION;
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g2d.setColor(foodColor);
            g2d.fillOval(x, y, size, size);
            g2d.dispose();
            eatAnimationTick++;
        }
    }
    
    // ... (Keep existing getters and animation logic) ...
    public void startEatingAnimation() { isBeingEaten = true; eatAnimationTick = 0; }
    public boolean isAnimationComplete() { return isBeingEaten && eatAnimationTick >= EAT_ANIMATION_DURATION; }
    public int getCenterX() { return x + size / 2; }
    public int getCenterY() { return y + size / 2; }
    public double getRadius() { return size / 2.0; }
    public FoodType getType() { return type; }
    public int getHungerValue() { return type.hungerRestore; }
}