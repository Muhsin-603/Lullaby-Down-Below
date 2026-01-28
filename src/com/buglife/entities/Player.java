package com.buglife.entities;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
// import javax.imageio.ImageIO;
import java.util.function.IntBinaryOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
 
import com.buglife.assets.SoundManager;
import com.buglife.assets.AssetManager;
import com.buglife.world.World;
import com.buglife.config.GameConstants;
import com.buglife.config.TileConstants;

public class Player {
    private static final Logger logger = LoggerFactory.getLogger(Player.class);
    // Player attributes
    public String facingDirection = "DOWN"; // Default
    private int dashDuration = 0;
    private int dashCooldown = 0;
    private double dashVelX = 0;
    private double dashVelY = 0;
    private boolean isDashing = false;
    private double x, y;
    private int width, height;
    private double currentSpeed; // How fast we are moving RIGHT NOW
    private int speedBoostTimer = 0;


    private int hunger = GameConstants.Player.MAX_HUNGER;
    private int collisionRadius;
    private int hungerDrainTimer = 0;
    private boolean isCrying = false;
    private int cryDeathTimer = 0; // Timer for death by hunger after crying starts
    private boolean isLowHungerWarningPlayed = false;
    private BufferedImage webbedSprite;
    private boolean onLevelCompleteTile = false;
    private int webbedWidth, webbedHeight;
    private Rectangle bounds;

    // This tracks hunger and crying mechanics

    // private BufferedImage sprite_walk1, sprite_walk2; // Just our two images
    private int animationTick = 0;
    private int currentFrame = 0;
    private PlayerState currentState = PlayerState.IDLE_DOWN;
    private static List<BufferedImage> idleDownFrames;
    private static List<BufferedImage> walkDownFrames;
    private static List<BufferedImage> walkUpFrames;
    private static List<BufferedImage> walkLeftFrames; // New list
    private static List<BufferedImage> walkRightFrames;
    private int webStrength = GameConstants.Player.WEB_ESCAPE_REQUIRED;
    private int webbedTimer = GameConstants.Player.WEBBED_DEATH_TIMER;
    private boolean diedFromWeb = false;

    public boolean isWebbed() {

        return this.currentState == PlayerState.WEBBED;
    }
    public int getSpeedBoostTimer() {
        return this.speedBoostTimer;
    }

    public boolean hasDiedFromWeb() {
        return this.diedFromWeb;
    }

    public enum PlayerState {
        IDLE_DOWN, // Standing, facing down
        WALKING_DOWN, WALKING_UP, WALKING_LEFT, // New state
        WALKING_RIGHT, WEBBED // New state
        // Maybe add IDLE_UP, IDLE_LEFT, IDLE_RIGHT later? For now, idle is just down.
    }

    public void eat(Food food) {
        // 1. Restore Hunger
        this.hunger += food.getHungerValue();
        if (this.hunger > GameConstants.Player.MAX_HUNGER) {
            this.hunger = GameConstants.Player.MAX_HUNGER;
        }

        // 2. Check Properties
        if (food.getType() == Food.FoodType.ENERGY_SEED) {
            // Apply 5 seconds of speed (60 fps * 5)
            this.speedBoostTimer = 300;
        }
    }
    // Add this method anywhere inside your Player class

    // In Player.java
    public void dash(int directionX, int directionY, SoundManager soundManager) {
        // Check if player has enough hunger and cooldown is ready
        if (dashCooldown <= 0 && !isDashing && hunger >= GameConstants.Player.DASH_HUNGER_COST) {
            // Normalize direction
            double length = Math.sqrt(directionX * directionX + directionY * directionY);
            if (length > 0) {
                dashVelX = (directionX / length) * GameConstants.Player.DASH_SPEED;
                dashVelY = (directionY / length) * GameConstants.Player.DASH_SPEED;
            } else {
                // If no direction, dash in the direction the player is facing
                switch (currentState) {
                    case WALKING_UP:
                        dashVelY = -GameConstants.Player.DASH_SPEED;
                        break;
                    case WALKING_DOWN:
                        dashVelY = GameConstants.Player.DASH_SPEED;
                        break;
                    case WALKING_LEFT:
                        dashVelX = -GameConstants.Player.DASH_SPEED;
                        break;
                    case WALKING_RIGHT:
                        dashVelX = GameConstants.Player.DASH_SPEED;
                        break;
                    default:
                        dashVelY = GameConstants.Player.DASH_SPEED; // Default down
                }
            }

            isDashing = true;
            dashDuration = GameConstants.Player.DASH_DURATION;
            dashCooldown = GameConstants.Player.DASH_COOLDOWN;

            // Consume hunger for the dash
            this.hunger -= GameConstants.Player.DASH_HUNGER_COST;
            if (this.hunger < 0) {
                this.hunger = 0;
            }

            soundManager.playSound("dash");
        }
    }

    public boolean isDashing() {
        return isDashing;
    }

    public void reset() {
        // Reset position
        this.x = 594.0; // Or whatever your default start position is
        this.y = 2484.0;

        // Reset hunger and crying state
        this.hunger = GameConstants.Player.MAX_HUNGER;
        this.isCrying = false;
        this.diedFromWeb = false;

        // Reset state to default idle
        this.currentState = PlayerState.IDLE_DOWN;
        this.currentFrame = 0; // Reset animation frame too
        this.animationTick = 0;

        // --- THE FIX ---
        // Reset web status completely
        this.webbedTimer = 0;
        this.webStrength = GameConstants.Player.WEB_ESCAPE_REQUIRED;
        // this.WEB_ESCAPE_REQUIRED = 4;

        // Make sure movement flags are off
        this.movingUp = false;
        this.movingDown = false;
        this.movingLeft = false;
        this.movingRight = false;
        // this.isFacingLeft = false; // Reset facing direction
        this.isDashing = false;
        this.dashDuration = 0;
        this.dashCooldown = 0;
        this.dashVelX = 0;
        this.dashVelY = 0;

        // game win
        this.onLevelCompleteTile = false;
    }

    // Add this method to Player.java
    public void getWebbed() {
        if (currentState != PlayerState.WEBBED) {
            currentState = PlayerState.WEBBED;

            webbedTimer = GameConstants.Player.WEBBED_DEATH_TIMER; // You have 5 seconds to live...
            webStrength = GameConstants.Player.WEB_ESCAPE_REQUIRED; // ...and 4 taps to escape. Good luck.
            this.currentFrame = 0;
        }
    }

    // Replace your existing render method with this one in Player.java
    public void render(Graphics g, World world) {
        Graphics2D g2d = (Graphics2D) g.create();
        try {
            // Check for shadow tile transparency
            int playerTileCol = getCenterX() / World.TILE_SIZE;
            int playerTileRow = getCenterY() / World.TILE_SIZE;
            if (world.getTileIdAt(playerTileCol, playerTileRow) == TileConstants.SHADOW_TILE) {
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
            }

            // Use webbed sprite if in webbed state
            if (currentState == PlayerState.WEBBED && webbedSprite != null) {
                int drawX = (int)x - (webbedWidth - width) / 2;
                int drawY = (int)y - (webbedHeight - height) / 2;
                g2d.drawImage(webbedSprite, drawX, drawY, webbedWidth, webbedHeight, null);
            } else {
                // ...existing animation rendering code...
                List<BufferedImage> currentAnimation = getActiveAnimation();
                if (currentAnimation != null && !currentAnimation.isEmpty()) {
                    BufferedImage imageToDraw = currentAnimation.get(currentFrame);
                    g2d.drawImage(imageToDraw, (int) x, (int) y, width, height, null);
                }
            }
        } finally {
            g2d.dispose();
        }
    }

    // Add this method to your Player.java class

    // The new, super-safe drawHitbox method in Player.java
    public void drawHitbox(Graphics g) {
        // 1. Create a disposable copy, JUST for the hitbox.
        Graphics2D g2d = (Graphics2D) g.create();

        // Get the center of the bug
        int centerX = getCenterX();
        int centerY = getCenterY();

        // Set the color to a semi-transparent red
        g2d.setColor(new Color(255, 0, 0, 100));

        // Draw the oval on our clean, disposable copy
        g2d.fillOval(centerX - collisionRadius, centerY - collisionRadius, collisionRadius * 2, collisionRadius * 2);

        // 2. Throw the copy away immediately.
        g2d.dispose();
    }

    // In Player.java

    public int getCenterX() {
        return (int) this.x + (this.width / 2);
    }

    public int getCenterY() {
        return (int) this.y + (this.height / 2);
    }

    public double getRadius() {
        return this.collisionRadius;
    }

    // In Player.java

    // Movement state flags
    public boolean movingUp, movingDown, movingLeft, movingRight;

    /**
     * Constructor for our heroic bug.
     * 
     * @param startX The initial X position.
     * @param startY The initial Y position.
     * @param size   The width and height of the player.
     */
    public Player(int startX, int startY, int drawSize, int collisionSize) {
        this.x = startX;
        this.y = startY;
        this.width = drawSize;
        this.height = drawSize;
        this.collisionRadius = collisionSize / 2;
        this.webbedWidth = (int)(width * 1.2);
        this.webbedHeight = (int)(height * 1.2);
        loadAnimations();
    }
    /// res/sprites/player/pla.png"

    private void loadAnimations() {
        if (idleDownFrames != null) {
            return;
        }
        idleDownFrames = new ArrayList<>();
        walkDownFrames = new ArrayList<>();
        walkUpFrames = new ArrayList<>();
        walkLeftFrames = new ArrayList<>();
        walkRightFrames = new ArrayList<>();

        try {
            // Load main sprite sheet
            BufferedImage spriteSheet = AssetManager.getInstance().loadImage("/res/sprites/player/pla.png");

            // Load webbed state sprite in the same try block
            webbedSprite = AssetManager.getInstance().loadImage("/res/sprites/player/webbed_state.png");
            if (webbedSprite == null) {
                logger.error("Failed to load webbed state sprite: /res/sprites/player/webbed_state.png");
            }

            // --- Sprite Dimensions ---
            final int SPRITE_WIDTH = 13;
            final int SPRITE_HEIGHT = 28;

            // --- !!! YOUR EXACT MEASUREMENTS !!! ---
            final int PADDING_LEFT = 25;
            final int PADDING_TOP = 15;
            final int HORIZONTAL_SPACING = 64;
            final int VERTICAL_SPACING = 64;

            // Your existing coordinate calculation helper
            IntBinaryOperator getX = (col, row) -> PADDING_LEFT + col * HORIZONTAL_SPACING;
            IntBinaryOperator getY = (col, row) -> PADDING_TOP + row * VERTICAL_SPACING;

            // --- THE DIGITAL SCISSORS - Perfectly Calibrated ---

            // Idle Down (Stand - Row index 0, Col index 0)
            idleDownFrames.add(
                    spriteSheet.getSubimage(getX.applyAsInt(0, 0), getY.applyAsInt(0, 0), SPRITE_WIDTH, SPRITE_HEIGHT));

            // Walk Down (Row index 4, Cols 0-3)
            for (int i = 0; i < 4; i++) {
                walkDownFrames.add(spriteSheet.getSubimage(getX.applyAsInt(i, 4), getY.applyAsInt(i, 4), SPRITE_WIDTH,
                        SPRITE_HEIGHT));
            }

            // Walk Up (Row index 5, Cols 0-3)
            for (int i = 0; i < 4; i++) {
                walkUpFrames.add(spriteSheet.getSubimage(getX.applyAsInt(i, 5), getY.applyAsInt(i, 5), SPRITE_WIDTH,
                        SPRITE_HEIGHT));
            }

            // Walk Right (Row index 6, Cols 0-3)
            for (int i = 0; i < 4; i++) {
                walkRightFrames.add(spriteSheet.getSubimage(getX.applyAsInt(i, 6), getY.applyAsInt(i, 6), SPRITE_WIDTH,
                        SPRITE_HEIGHT));
            }

            // Walk Left (Row index 7, Cols 0-3)
            for (int i = 0; i < 4; i++) {
                walkLeftFrames.add(spriteSheet.getSubimage(getX.applyAsInt(i, 7), getY.applyAsInt(i, 7), SPRITE_WIDTH,
                        SPRITE_HEIGHT));
            }

        } catch (Exception e) {
            logger.error("Failed to load player sprites or slice sprite sheet", e);
        }
    }

    // Add this method to Player.java
    public void struggle() {
        if (currentState == PlayerState.WEBBED) {
            webStrength--;

            if (webStrength <= 0) {
                currentState = PlayerState.IDLE_DOWN;
                webbedTimer = GameConstants.Player.WEBBED_DEATH_TIMER;
                webStrength = GameConstants.Player.WEB_ESCAPE_REQUIRED;
            }
        }
    }

    private void checkLowHunger(SoundManager soundManager) {
        int hungerPercentage = (hunger * 100) / GameConstants.Player.MAX_HUNGER;

        if (hungerPercentage <= GameConstants.Player.LOW_HUNGER_THRESHOLD && !isLowHungerWarningPlayed) {
            soundManager.playSound("lowhunger");
            isLowHungerWarningPlayed = true;
        } else if (hungerPercentage > GameConstants.Player.LOW_HUNGER_THRESHOLD) {
            isLowHungerWarningPlayed = false;
        }
    }

    private void updateHungerAndCrying(SoundManager soundManager) {
        if (isCrying) {
            cryDeathTimer--;
            if (cryDeathTimer <= 0) {
                this.hunger = 0;
            }
        } else {
            hungerDrainTimer++;
            if (hungerDrainTimer > 120) {
                this.hunger--;
                hungerDrainTimer = 0;
                if (this.hunger <= 0) {
                    this.hunger = 0;
                    if (!isCrying) {
                        this.isCrying = true;
                        this.cryDeathTimer = GameConstants.Player.CRY_DEATH_DURATION;
                    }
                }
            }
        }
        checkLowHunger(soundManager);
    }

    /**
     * Updates the player's position based on movement flags. This will be called in
     * the main game loop.
     */
    // The new update method now takes the World as an argument
    public void update(World world, SoundManager soundManager) {
        // --- First, check for paralysis ---
        // Inside Player.java's update() method...

        if (speedBoostTimer > 0) {
            this.currentSpeed = GameConstants.Player.BOOST_SPEED; // Use the boost speed
            speedBoostTimer--;               // Tick down the timer
        } else {
            // Normal logic when not boosted
            int playerTileCol = getCenterX() / World.TILE_SIZE;
            int playerTileRow = getCenterY() / World.TILE_SIZE;
            int tileID = world.getTileIdAt(playerTileCol, playerTileRow);
            
            if (tileID == TileConstants.STICKY_FLOOR) { // Sticky floor
                this.currentSpeed = GameConstants.Player.SLOW_SPEED;
            } else {
                this.currentSpeed = GameConstants.Player.NORMAL_SPEED;
            }
        }
        
        int playerTileCol = getCenterX() / World.TILE_SIZE;
        int playerTileRow = getCenterY() / World.TILE_SIZE;

        if (world.getTileIdAt(playerTileCol, playerTileRow) == TileConstants.LADDER_3) {
            this.onLevelCompleteTile = true;
        } else {
            this.onLevelCompleteTile = false; // Ensure it's false when not on the tile
        }
        if (dashCooldown > 0) {
            dashCooldown--;
        }
        if (dashDuration > 0) {
            dashDuration--;
        } else {
            isDashing = false;
        }

        // If currently dashing, apply dash velocity
        if (isDashing) {
            x += dashVelX;
            y += dashVelY;

            // Check collision with walls during dash using the new checkCollision method
            if (world.checkCollision((int) x, (int) y, width, height)) {
                // Hit a wall, revert movement and stop dash
                x -= dashVelX;
                y -= dashVelY;
                isDashing = false;
                dashDuration = 0;
            }
            return; // Skip normal movement while dashing
        }

        if (currentState == PlayerState.WEBBED) {
            if (isCrying) {
                this.hunger = 0;
            } else {
                webbedTimer--;
                if (webbedTimer <= 0) {
                    this.diedFromWeb = true;
                }
                return;
            }
        }
        updateHungerAndCrying(soundManager);

        if (currentState != PlayerState.WEBBED) {
            // --- If we are NOT webbed, proceed with normal life ---

            // 1. State Management: Decide which animation to play.
            PlayerState previousState = currentState;

            if (movingUp) {
                currentState = PlayerState.WALKING_UP;
                facingDirection = "UP"; // <--- Add this
            } else if (movingDown) {
                currentState = PlayerState.WALKING_DOWN;
                facingDirection = "DOWN"; // <--- Add this
            } else if (movingLeft) {
                currentState = PlayerState.WALKING_LEFT;
                facingDirection = "LEFT"; // <--- Add this
            } else if (movingRight) {
                currentState = PlayerState.WALKING_RIGHT;
                facingDirection = "RIGHT"; // <--- Add this
            } else {
                // If not moving, stay idle (facing down for now)
                currentState = PlayerState.IDLE_DOWN;
            }

            if (previousState != currentState) {
                currentFrame = 0;
                animationTick = 0;
            }

            // 2. Movement & Wall Collision
            // (Your existing wall collision logic is perfect here)
            double nextX = x, nextY = y;
            if (movingUp)
                nextY -= currentSpeed;
            if (movingDown)
                nextY += currentSpeed;
            if (movingLeft)
                nextX -= currentSpeed;
            if (movingRight)
                nextX += currentSpeed;

            if (nextX != x || nextY != y) {
                int nextLeft = (int) nextX;
                int nextRight = (int) nextX + width - 1;
                int nextTop = (int) nextY;
                int nextBottom = (int) nextY + height - 1;

                if (!world.isTileSolid(nextLeft, nextTop) && !world.isTileSolid(nextRight, nextTop)
                        && !world.isTileSolid(nextLeft, nextBottom) && !world.isTileSolid(nextRight, nextBottom)) {
                    x = nextX;
                    y = nextY;
                }
            }
            // 4. Animation
            boolean isMoving = movingUp || movingDown || movingLeft || movingRight;
            if (isMoving) {
                animationTick++;
                if (animationTick > GameConstants.Player.ANIMATION_SPEED) {
                    animationTick = 0;
                    currentFrame = (currentFrame + 1) % getActiveAnimation().size();
                }
            } else {
                currentFrame = 0;
            }
        }
    }

    public String getFacingDirection() {
        return facingDirection;
    }

    public boolean isOnLevelCompleteTile() {
        return this.onLevelCompleteTile;
    }

    private List<BufferedImage> getActiveAnimation() {
        switch (currentState) {
            case WALKING_UP:
                return walkUpFrames;
            case WALKING_DOWN:
                return walkDownFrames;
            case WALKING_LEFT:
                return walkLeftFrames; // Use the new list
            case WALKING_RIGHT:
                return walkRightFrames; // Use the new list
            case IDLE_DOWN:
            default:
                return idleDownFrames;
        }
    }

    /**
     * Draws the player on the screen.
     * 
     * 
     */

    public int getX() {
        return (int) this.x;
    }

    public int getY() {
        return (int) this.y;
    }

    /**
     * Returns the player's collision bounds. Super useful later!
     * 
     * @return A Rectangle object representing the player's position and size.
     */
    public Rectangle getBounds() {
        bounds.setLocation((int)x, (int)y); // Update and return the same object
        return bounds;
    }

    public void decreaseHunger(int amount) {
        this.hunger -= amount;
        if (hunger < 0)
            hunger = 0;
    }

    public void takeDamage() {
        // For future implementation of damage mechanics
        this.isCrying = true;
    }

    public int getHunger() {
        return this.hunger;
    }

    public boolean isCrying() {
        return this.isCrying;
    }
}