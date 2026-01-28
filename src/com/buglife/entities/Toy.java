package src.com.buglife.entities;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;

import src.com.buglife.world.World;

public class Toy {
    private double x, y;
    private int width = 24, height = 24;
    private boolean active = false;
    private boolean makingNoise = false;
    private int noiseTimer = 0;
    private double velX, velY;
    private final int NOISE_DURATION = 180; 
    private final double THROW_SPEED = 15.0;
    
    private BufferedImage sprite;
    
    // --- ADD THESE NEW VARIABLES ---
    private double spawnX, spawnY;
    private boolean isSpawned = false;
    private boolean isCarried = false; // New: track if player is carrying it
    // private double carriedOffsetX = 0, carriedOffsetY = 0;

    public Toy() {
        loadSprite();
    }
    
    // --- ADD THIS NEW METHOD ---
    public void setSpawnLocation(int tileX, int tileY) {
        this.spawnX = tileX * World.TILE_SIZE + (World.TILE_SIZE / 4);
        this.spawnY = tileY * World.TILE_SIZE + (World.TILE_SIZE / 4);
        this.x = spawnX;
        this.y = spawnY;
        this.isSpawned = true;
        this.active = false; // Toy is waiting to be thrown
    }
    
    // --- ADD THIS OVERLOADED VERSION ---
    public void setSpawnLocationPixels(int pixelX, int pixelY) {
        this.spawnX = pixelX;
        this.spawnY = pixelY;
        this.x = spawnX;
        this.y = spawnY;
        this.isSpawned = true;
        this.active = false;
    }

    private void loadSprite() {
        try {
            sprite = ImageIO.read(getClass().getResourceAsStream("/res/sprites/items/toy1.png"));
        } catch (IOException | NullPointerException e) {
            System.err.println("Error loading toy sprite! Drawing fallback box.");
            e.printStackTrace();
            sprite = null;
        }
    }

    public void throwToy(double startX, double startY, String direction) {
        this.x = startX;
        this.y = startY;
        this.active = true;
        this.makingNoise = true;
        this.noiseTimer = NOISE_DURATION;
        this.isCarried = false;

        velX = 0; velY = 0;
        if (direction.equals("UP")) velY = -THROW_SPEED;
        else if (direction.equals("DOWN")) velY = THROW_SPEED;
        else if (direction.equals("LEFT")) velX = -THROW_SPEED;
        else if (direction.equals("RIGHT")) velX = THROW_SPEED;
    }

        public void update() {
        if (!active) return;

        x += velX;
        y += velY;
        velX *= 0.9;
        velY *= 0.9;

        if (Math.abs(velX) < 0.1 && Math.abs(velY) < 0.1) {
            velX = 0;
            velY = 0;
        }

        if (makingNoise) {
            noiseTimer--;
            if (noiseTimer <= 0) {
                makingNoise = false;
                active = false;
                // --- CHANGED: Don't return to spawn, stay at thrown location ---
                // This allows the player to pick it up from where it landed
            }
        }
    }
    public void drawInteractionPrompt(Graphics g) {
        if (isCarried || !isSpawned || !canPickUp(null)) return; // Don't show if carried or can't interact
        
        g.setColor(new Color(255, 255, 255));
        g.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 14));
        String prompt = "Press E to pick up";
        java.awt.FontMetrics fm = g.getFontMetrics();
        int promptWidth = fm.stringWidth(prompt);
        g.drawString(prompt, getCenterX() - promptWidth/2, (int)y - 10);
    }

    public void draw(Graphics g) {
        // Don't draw if not spawned OR if being carried
        if (!isSpawned || isCarried) return;
        
        if (sprite != null) {
            g.drawImage(sprite, (int)x, (int)y, width, height, null);
        } else {
            g.setColor(Color.CYAN);
            g.fillOval((int)x, (int)y, width, height);
        }
        
        if (makingNoise) {
            g.setColor(new Color(0, 255, 255, 100));
            int rippleSize = width * 2 + (noiseTimer % 20);
            g.drawOval((int)getCenterX() - rippleSize/2, (int)getCenterY() - rippleSize/2, rippleSize, rippleSize);
        }
    }
    public void pickUp(Player player) {
        this.isCarried = true;
        this.active = false;
        this.makingNoise = false;
    }

    public void drop(Player player) {
        this.isCarried = false;
        this.x = player.getCenterX();
        this.y = player.getCenterY();
    }

    public boolean isCarried() {
        return this.isCarried;
    }

    public boolean canPickUp(Player player) {
        if (isCarried || isSpawned == false) return false;
        if (player == null) return true;
        
        double dx = player.getCenterX() - getCenterX();
        double dy = player.getCenterY() - getCenterY();
        double distance = Math.sqrt(dx * dx + dy * dy);
        
        return distance < 50; // Interaction radius
    }

    public boolean isActive() { return active; }
    public boolean isMakingNoise() { return active && makingNoise; }
    public int getCenterX() { return (int)x + width / 2; }
    public int getCenterY() { return (int)y + height / 2; }
}