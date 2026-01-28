package com.buglife.entities;

// import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import com.buglife.assets.SoundManager;
import com.buglife.assets.AssetManager;
//import com.buglife.entities.Player.PlayerState;
import com.buglife.world.World;
import java.awt.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.buglife.config.TileConstants;

public class Spider {
    private static final Logger logger = LoggerFactory.getLogger(Spider.class);
    // Core Attributes
    private double x, y;
    private int width = 48, height = 48;
    private double speed;
    private final double PATROL_SPEED = 1; // default patrol movement speed (pixels/frame)
    private final double CHASE_SPEED = 3; // speed when actively chasing (adjust this)
    private final double SLOW_CHASE_SPEED = 1.7; // slow chase speed (used when not "crying")
    private final double INVESTIGATE_SPEED = 3.5;

    private double rotationAngle = 90; // Start facing right (90 degrees from North)

    private Point investigationPoint;
    private int investigationTimer;

    // Animation Reel (for the 2-frame bug)
    private BufferedImage[] walkingFrames;
    private final int TOTAL_FRAMES = 2;
    private int currentFrame = 0;
    private int animationTick = 0;
    private int animationSpeed = 5;
    private List<Point> patrolPath;
    private int currentTargetIndex = 0;
    private boolean isMovingForward = true;
    // Add these new fields to Spider.java
    // private List<Point> breadcrumbTrail; // The memory of the chase
    private Point returnPoint;
    private int loseSightTimer; // A countdown for when it loses the player
    private Player targetPlayer; // A reference to the player it's hunting

    // Inside the Spider.java class
    public enum SpiderState {
        PATROLLING, // The default, boring patrol
        CHASING, // The "I see you!" bloodlust mode
        RETURNING, // The "Where did he go?" confused mode
        DISTRACTED,
        INVESTIGATING
    }

    private SpiderState currentState; // A variable to hold the spider's current mood

    public void setReturnPoint(Point p) {
        this.returnPoint = p;
    }

    public Spider(List<Point> tilePath) {
        // Convert the tile-based path to a pixel-based path
        this.patrolPath = new ArrayList<>();
        this.currentState = SpiderState.PATROLLING;
        // this.breadcrumbTrail = new ArrayList<>();
        this.returnPoint = new Point();
        for (Point tilePoint : tilePath) {
            int pixelX = tilePoint.x * World.TILE_SIZE + (World.TILE_SIZE / 2);
            int pixelY = tilePoint.y * World.TILE_SIZE + (World.TILE_SIZE / 2);
            this.patrolPath.add(new Point(pixelX, pixelY));
        }

        // Spawn at the first point in the path
        if (!this.patrolPath.isEmpty()) {
            this.x = this.patrolPath.get(0).x - (width / 2);
            this.y = this.patrolPath.get(0).y - (height / 2);
        }

        // set default speed
        this.speed = PATROL_SPEED;

        loadSprites();
    }

    // This method checks if the spider can see the player.
    private boolean canSeePlayer(Player player, World world) {
        if (player == null)
            return false;

        // 1. Simple distance check first (is the player even close enough?)
        double dx = player.getCenterX() - getCenterX();
        double dy = player.getCenterY() - getCenterY();
        double distance = Math.sqrt(dx * dx + dy * dy);
        int playerTileCol = player.getCenterX() / World.TILE_SIZE;
        int playerTileRow = player.getCenterY() / World.TILE_SIZE;
        if (world.getTileIdAt(playerTileCol, playerTileRow) == TileConstants.SHADOW_TILE) { // Is it a shadow tile?
            return false; // I can't see anything!
        }

        int detectionRadius = 200; // How far the spider can see
        if (distance > detectionRadius) {
            return false;
        }

        // 2. Line-of-sight check (is a wall in the way?)
        // We'll check a few points on the line between the spider and player.
        int steps = (int) (distance / (World.TILE_SIZE / 2)); // Check every half-tile
        for (int i = 1; i < steps; i++) {
            int checkX = (int) (getCenterX() + (dx / steps) * i);
            int checkY = (int) (getCenterY() + (dy / steps) * i);
            if (world.isTileSolid(checkX, checkY)) {
                return false; // A wall is in the way!
            }
        }

        return true; // The path is clear! I SEE YOU!
    }

    private void loadSprites() {
        walkingFrames = new BufferedImage[TOTAL_FRAMES];
        try {
            // Load the original bug sprites
            walkingFrames[0] = AssetManager.getInstance().loadImage("/res/sprites/spider/Walk_0001.png");
            walkingFrames[1] = AssetManager.getInstance().loadImage("/res/sprites/spider/Walk_0002.png");
        } catch (Exception e) {
            logger.error("Failed to load spider sprites", e);
        }
    }
    // In Spider.java

    public SpiderState getCurrentState() {
        return this.currentState;
    }

    // Method to force the spider into chase mode when alerted
    public void startChasing(Player targetPlayer, SoundManager soundManager) {
        if (currentState == SpiderState.PATROLLING || currentState == SpiderState.RETURNING) { // Only switch if
                                                                                               // currently patrolling
            this.targetPlayer = targetPlayer; // Make sure it knows who to chase
            this.returnPoint = new Point(getCenterX(), getCenterY()); // Set return point
            this.currentState = SpiderState.CHASING;
            soundManager.stopSound("music");
            soundManager.playSound("chasing");
        }
    }

    // Add this method anywhere inside your Spider class

    public void reset() {
        // Teleport back to the first point in the patrol path
        if (patrolPath != null && !patrolPath.isEmpty()) {
            this.x = patrolPath.get(0).x - (width / 2.0);
            this.y = patrolPath.get(0).y - (height / 2.0);
        }

        // Reset the AI's brain to its initial state
        this.currentTargetIndex = 0; // Immediately target the second point to start moving
        this.isMovingForward = true;

        // Reset the animation to the first frame
        this.currentFrame = 0;
        this.animationTick = 0;
    }

    // In Spider.java

    public void hearNoise(Point noiseLocation, int soundRadius) {
        // Ignore noise if we are already busy killing the player
        if (currentState != SpiderState.CHASING) {
            double dist = Math.sqrt(Math.pow(noiseLocation.x - getCenterX(), 2) + Math.pow(noiseLocation.y - getCenterY(), 2));
            
            // Use the Trip Wire's specific radius
            if (dist < soundRadius) { 
                // CRITICAL: Only update 'returnPoint' if we were patrolling.
                // If we were already investigating or returning, we want to remember 
                // our ORIGINAL patrol post, not the random spot we are standing in now.
                if (currentState == SpiderState.PATROLLING) {
                    this.returnPoint = new Point(getCenterX(), getCenterY());
                }
                
                this.investigationPoint = noiseLocation;
                this.currentState = SpiderState.INVESTIGATING;
                this.investigationTimer = 300; // 5 Seconds (60 frames * 5)
            }
        }
    }
    

    // In Spider.java, replace the entire update method.
    // In Spider.java

    public void update(Player player, World world, SoundManager soundManager, Toy toy) {
        this.targetPlayer = player;

        if (player.isCrying()) {
            // Ignore everything else. KILL.
            this.returnPoint = new Point(getCenterX(), getCenterY());
            currentState = SpiderState.CHASING;
            // (Your existing crying chase logic...)
            speed = 3; // Fast!
            chase(targetPlayer);
            return; // Skip the rest of the state machine
        }
        if (toy != null && toy.isMakingNoise()) {
            double dxToy = toy.getCenterX() - getCenterX();
            double dyToy = toy.getCenterY() - getCenterY();
            double distanceToToy = Math.sqrt(dxToy * dxToy + dyToy * dyToy);

            // If toy is within detection radius and not already chasing player closely
            if (distanceToToy < 250) { // Toy detection radius
                if (currentState != SpiderState.CHASING || distanceToToy < 150) {
                    currentState = SpiderState.DISTRACTED;
                    this.returnPoint = new Point(getCenterX(), getCenterY());
                    soundManager.stopSound("chasing");
                }
            }
        }
        // The State Machine: The spider's brain.
        switch (currentState) {
            case PATROLLING:
                doPatrol(world);
                // While patrolling, constantly look for the player.
                if (canSeePlayer(targetPlayer, world)) {
                    // Drop a GPS pin at our current location. THIS is our post.
                
                    this.returnPoint = new Point(getCenterX(), getCenterY());
                    currentState = SpiderState.CHASING;
                    soundManager.stopSound("music");
                    soundManager.playSound("chasing");
                    loseSightTimer = 300;
                }
                break;

            case INVESTIGATING:
                // --- THE GHOST LOGIC ---
                speed = INVESTIGATE_SPEED;
                
                double dxNoise = investigationPoint.x - getCenterX();
                double dyNoise = investigationPoint.y - getCenterY();
                double distNoise = Math.sqrt(dxNoise * dxNoise + dyNoise * dyNoise);

                if (distNoise > 5) {
                    // Move DIRECTLY to noise. NO WALL CHECKS.
                    double moveX = (dxNoise / distNoise) * speed;
                    double moveY = (dyNoise / distNoise) * speed;
                    
                    x += moveX;
                    y += moveY;
                    rotationAngle = Math.toDegrees(Math.atan2(moveY, moveX)) + 90;
                    
                } else {
                    // Arrived. Wait 5 seconds.
                    investigationTimer--;
                    if (investigationTimer <= 0) {
                        // Time's up. Nothing here. Return to original post.
                        currentState = SpiderState.RETURNING;
                    }
                }
                break;
            case DISTRACTED:
                // Go to the toy!
                soundManager.stopSound("music");
                if (toy != null && toy.isMakingNoise()) {
                    double dx = toy.getCenterX() - getCenterX();
                    double dy = toy.getCenterY() - getCenterY();
                    double dist = Math.sqrt(dx * dx + dy * dy);
                    speed = SLOW_CHASE_SPEED;

                    
                    // soundManager.playSound("chasing");

                    if (dist > 10) {
                        // Move towards toy
                        double moveX = (dx / dist) * speed;
                        double moveY = (dy / dist) * speed;

                        // Check wall collision
                        double nextX = x + moveX;
                        double nextY = y + moveY;
                        int nextLeft = (int) nextX;
                        int nextRight = (int) nextX + width - 1;
                        int nextTop = (int) nextY;
                        int nextBottom = (int) nextY + height - 1;

                        if (!world.isTileSolid(nextLeft, nextTop) && !world.isTileSolid(nextRight, nextTop)
                                && !world.isTileSolid(nextLeft, nextBottom)
                                && !world.isTileSolid(nextRight, nextBottom)) {
                            x += moveX;
                            y += moveY;
                        }

                        rotationAngle = Math.toDegrees(Math.atan2(moveY, moveX)) + 90;
                    }
                    if (canSeePlayer(targetPlayer, world)) {
                        double dxPlayer = targetPlayer.getCenterX() - getCenterX();
                        double dyPlayer = targetPlayer.getCenterY() - getCenterY();
                        double distToPlayer = Math.sqrt(dxPlayer * dxPlayer + dyPlayer * dyPlayer);

                        if (distToPlayer < 80) {
                            currentState = SpiderState.CHASING;
                            soundManager.stopSound("music");
                            soundManager.playSound("chasing");
                            loseSightTimer = 300;
                        }
                    } else {
                        currentState = SpiderState.RETURNING;
                        this.returnPoint = new Point(getCenterX(), getCenterY());
                    }

                    // While distracted, if we literally bump into the player, we should probably
                    // attack?
                    // But for now, let's say the noise is overpowering.
                    break;
                }

            case CHASING:
                // --- NEW "MISSION ACCOMPLISHED" CHECK ---
                // First, check if our target is already webbed.
                if (targetPlayer.isWebbed()) {
                    currentState = SpiderState.RETURNING;// My job here is done.
                    soundManager.stopSound("chasing");
                    soundManager.playSound("music");
                    break; // Immediately exit the CHASING logic.
                }

                if (player.isCrying()) {
                    // previously: speed = 3;
                    speed = CHASE_SPEED;
                    chase(targetPlayer);
                } else {
                    // previously: speed = 1;
                    speed = SLOW_CHASE_SPEED;
                    // If the player isn't webbed, continue the hunt as normal.
                    if (canSeePlayer(targetPlayer, world)) {
                        chase(targetPlayer);
                        loseSightTimer = 300;
                    } else {
                        loseSightTimer--;
                        if (loseSightTimer <= 0) {
                            currentState = SpiderState.RETURNING;
                            soundManager.stopSound("chasing");
                            soundManager.playSound("music");
                        }
                    }
                }
                break;
            case RETURNING:
                // Check if we've made it back to our post.
                speed = PATROL_SPEED;
                double dx = returnPoint.x - getCenterX();
                double dy = returnPoint.y - getCenterY();
                double distanceToPost = Math.sqrt(dx * dx + dy * dy);

                if (distanceToPost < 5) {
                    // We're back! Resume normal patrol.
                    currentState = SpiderState.PATROLLING;
                    soundManager.stopSound("chasing");
                    soundManager.playSound("music");
                } else {
                    // If not, take the shortest path back.
                    returnToPost();
                }
                if (canSeePlayer(targetPlayer, world)) {
                    currentState = SpiderState.CHASING;
                    soundManager.stopSound("music");
                    soundManager.playSound("chasing"); // Play sound when seeing player while returning
                    loseSightTimer = 300;
                }
                break;
        }

        animationTick++;
        if (animationTick > animationSpeed) {
            animationTick = 0;
            currentFrame = (currentFrame + 1) % TOTAL_FRAMES;
        }
    }

    public void draw(Graphics g) {
        // We will use the simple draw method for now to be safe
        BufferedImage imageToDraw = walkingFrames[currentFrame];
        if (imageToDraw != null) {
            Graphics2D g2d = (Graphics2D) g.create();
            try {
                g2d.rotate(Math.toRadians(this.rotationAngle), this.getCenterX(), this.getCenterY());
                g2d.drawImage(imageToDraw, (int) this.x, (int) this.y, this.width, this.height, null);
            } finally {
                g2d.dispose();
            }
        } else {
            // Failsafe so we can see it even if sprites are null
            g.setColor(Color.MAGENTA);
            g.fillRect((int) this.x, (int) this.y, this.width, this.height);
        }
    }

    private void returnToPost() {
        double dx = returnPoint.x - getCenterX();
        double dy = returnPoint.y - getCenterY();
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (distance > 1) {
            // Move directly towards our saved return point.
            double moveX = (dx / distance) * speed;
            double moveY = (dy / distance) * speed;
            x += moveX;
            y += moveY;
            rotationAngle = Math.toDegrees(Math.atan2(moveY, moveX)) + 90;
        }
    }

    // Add this simple method to Spider.java
    public boolean isChasing() {
        return this.currentState == SpiderState.CHASING;
    }

    private void chase(Player player) {
        // Simple "move towards player" logic
        double dx = player.getCenterX() - getCenterX();
        double dy = player.getCenterY() - getCenterY();
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (distance > 1) {
            double moveX = (dx / distance) * speed;
            double moveY = (dy / distance) * speed;
            x += moveX;
            y += moveY;
            rotationAngle = Math.toDegrees(Math.atan2(moveY, moveX)) + 90;
        }
    }

    private void doPatrol(World world) {
        if (patrolPath == null || patrolPath.isEmpty() || patrolPath.size() < 2) {
            return; // Can't patrol without at least two points.
        }
        Point target = patrolPath.get(currentTargetIndex);
        double dx = target.x - getCenterX();
        double dy = target.y - getCenterY();
        double distance = Math.sqrt(dx * dx + dy * dy);

        // 2. Check if we've arrived at our destination.
        if (distance < 5) { // The "close enough" rule
            // We've arrived! Decide where to go next based on our direction.
            if (isMovingForward) {
                currentTargetIndex++; // Move to the next point in the list.
                if (currentTargetIndex >= patrolPath.size()) {
                    // We've hit the end! Time to turn back.
                    isMovingForward = false;
                    currentTargetIndex = patrolPath.size() - 2; // Target the second-to-last point.
                }
            } else { // We are moving backward.
                currentTargetIndex--; // Move to the previous point.
                if (currentTargetIndex < 0) {
                    // We've hit the beginning! Time to turn back again.
                    isMovingForward = true;
                    currentTargetIndex = 1; // Target the second point.
                }
            }
            return; // Get a fresh start on the next frame.
        }

        // 3. If we haven't arrived, calculate movement.
        double moveX = (dx / distance) * speed;
        double moveY = (dy / distance) * speed;

        // 4. THE CONSCIENCE: Check for walls before moving.
        double nextX = x + moveX;
        double nextY = y + moveY;

        int nextLeft = (int) nextX;
        int nextRight = (int) nextX + width - 1;
        int nextTop = (int) nextY;
        int nextBottom = (int) nextY + height - 1;

        if (!world.isTileSolid(nextLeft, nextTop) && !world.isTileSolid(nextRight, nextTop)
                && !world.isTileSolid(nextLeft, nextBottom) && !world.isTileSolid(nextRight, nextBottom)) {
            // Path is clear! Commit the move and update rotation.
            x = nextX;
            y = nextY;
            this.rotationAngle = Math.toDegrees(Math.atan2(moveY, moveX)) + 90;
        } else {
            // Hit a wall! Skip to the next waypoint to try and get unstuck.
            currentTargetIndex = (currentTargetIndex + 1) % patrolPath.size();
        }

    }

    public double getRadius() {
        // My radius is just half my width!
        return width / 2.0;
    }

    // Collision Helpers
    // Helper methods now cast the double to an int just before returning
    public int getX() {
        return (int) this.x;
    }

    public int getY() {
        return (int) this.y;
    }

    public int getCenterX() {
        return (int) this.x + width / 2;
    }

    public int getCenterY() {
        return (int) this.y + height / 2;
    }

    public void setSpawnTile(Point tilePoint) {
        // Set spider spawn using tile coordinates (column,row)
        int pixelX = tilePoint.x * World.TILE_SIZE + (World.TILE_SIZE / 2);
        int pixelY = tilePoint.y * World.TILE_SIZE + (World.TILE_SIZE / 2);
        this.x = pixelX - (width / 2.0);
        this.y = pixelY - (height / 2.0);

        if (this.patrolPath == null)
            this.patrolPath = new ArrayList<>();
        if (this.patrolPath.isEmpty()) {
            this.patrolPath.add(new Point(pixelX, pixelY));
        } else {
            this.patrolPath.set(0, new Point(pixelX, pixelY)); // update first patrol point
        }
    }

    public void setSpawnPixel(int pixelX, int pixelY) {
        this.x = pixelX - (width / 2.0);
        this.y = pixelY - (height / 2.0);
        if (this.patrolPath == null)
            this.patrolPath = new ArrayList<>();
        if (this.patrolPath.isEmpty()) {
            this.patrolPath.add(new Point(pixelX, pixelY));
        } else {
            this.patrolPath.set(0, new Point(pixelX, pixelY));
        }
    }
}
