package com.buglife.entities;

import com.buglife.world.World;
import com.buglife.assets.AssetManager;
// import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;

import java.util.List;
import java.util.ArrayList;

public class Snail {

    private double x, y;
    private int width = 35; // Use the Idle dimensions
    private int height = 55;

    private static final Font DIALOG_FONT = new Font("Arial", Font.PLAIN, 16);
    private static final Font DIALOG_PROMPT_FONT = new Font("Arial", Font.ITALIC, 12);
    private static final Font INTERACTION_FONT = new Font("Arial", Font.BOLD, 14);
    private static final Color DIALOG_BG_COLOR = new Color(0, 0, 0, 200);

    // --- Simplified Animation ---
    private List<BufferedImage> idleFrames;
    private int currentFrame = 0;
    private int animationTick = 0;
    private int animationSpeed = 20; // Snails are slow
    private Player player; // Reference to the player

    // --- State ---
    private boolean isVisible = true;
    private List<SnailLocation> locations;
    private int currentLocationIndex = 0;

    // --- Glow effect ---

    // Add new fields for NPC behavior
    private static final int INTERACTION_RADIUS = 50;

    private int currentDialogue = 0;
    // private boolean isInteracting = false;
    private boolean showingDialog = false;

    // --- 2. THE NEW, SIMPLER CONSTRUCTOR ---
    public Snail(Player player, List<SnailLocation> locations) {
        this.player = player;
        this.locations = locations;

        if (locations != null && !locations.isEmpty()) {
            // Set initial state from the first location in the list
            teleportToLocation(0);
        } else {
            this.isVisible = false; // Hide if no locations are defined
        }

        loadAnimations();
    }

    public void teleportToLocation(int locationIndex) {
        if (locations == null || locationIndex < 0 || locationIndex >= locations.size()) {
            System.err.println("Invalid snail location index: " + locationIndex);
            return;
        }

        this.currentLocationIndex = locationIndex;
        SnailLocation newLocation = locations.get(currentLocationIndex);

        setPosition(newLocation.position().x, newLocation.position().y);

        closeDialog(); // Reset dialogue state
        show(); // Ensure it's visible
    }

    private void loadAnimations() {
        idleFrames = new ArrayList<>();
        try {
            BufferedImage spriteSheet = AssetManager.getInstance().loadImage("/res/sprites/snail/snail.png");
            if (spriteSheet == null) {
                System.err.println("Failed to load snail sprite sheet!");
                return;
            }

            // --- Slicing ONLY the Idle Animation (from 143x224 sheet) ---
            final int IDLE_WIDTH = 35, IDLE_HEIGHT = 55;
            final int IDLE_START_X = 0; // Top-left of sheet
            final int IDLE_START_Y = 0;
            final int IDLE_H_STEP = 35 + 17; // 52
            final int IDLE_FRAMES = 3; // The first row has 3 frames

            for (int i = 0; i < IDLE_FRAMES; i++) {
                int frameX = IDLE_START_X + i * IDLE_H_STEP;
                idleFrames.add(spriteSheet.getSubimage(frameX, IDLE_START_Y, IDLE_WIDTH, IDLE_HEIGHT));
            }

        } catch (Exception e) {
            System.err.println("CRASH! Could not slice snail idle frames. Check measurements!");
            e.printStackTrace();
        }
    }

    // Update now just animates the idle loop
    public void update(World world) {
        animationTick++;
        if (animationTick > animationSpeed) {
            animationTick = 0;
            if (idleFrames != null && !idleFrames.isEmpty()) {
                currentFrame = (currentFrame + 1) % idleFrames.size();
            }
        }
    }

    public void draw(Graphics g) {
        if (!isVisible) {
            return;
        }

        Graphics2D g2d = (Graphics2D) g.create();
        try {
            

            // Draw snail sprite
            if (idleFrames != null && !idleFrames.isEmpty()) {
                BufferedImage currentSprite = idleFrames.get(currentFrame);
                g2d.drawImage(currentSprite, (int) x, (int) y, width, height, null);
            } else {
                // Debug rectangle if sprite fails to load
                g2d.setColor(Color.MAGENTA);
                g2d.fillRect((int) x, (int) y, width, height);
            }
            if (showingDialog) {
                drawDialogBox(g2d);
            } else if (canInteract(player)) {
                drawInteractionPrompt(g2d);
            }
        } finally {
            g2d.dispose();
        }
    }

    // --- 3. THE MISSING METHODS ---
    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public boolean isVisible() {
        return this.isVisible;
    }

    public void hide() {
        this.isVisible = false;
    }

    public void show() {
        this.isVisible = true;
    }

    // Helper methods
    public int getX() {
        return (int) x;
    }

    public int getY() {
        return (int) y;
    }

    public int getWidth() {
        return width;
    } // <-- Missing method

    public int getHeight() {
        return height;
    } // <-- Missing method

    public int getCenterX() {
        return (int) x + width / 2;
    }

    public int getCenterY() {
        return (int) y + height / 2;
    }

    public record SnailLocation(Point position, String[] dialogues, boolean requiresInteraction) {
    }

    // Add new method for interaction check
    public boolean canInteract(Player player) {
        if (player == null) {
            return false; // Can't interact with nothing
        }
        double dx = player.getCenterX() - getCenterX();
        double dy = player.getCenterY() - getCenterY();
        double distance = Math.sqrt(dx * dx + dy * dy);
        return distance <= INTERACTION_RADIUS;
    }

    public SnailLocation getCurrentLocation() {
        if (locations != null && !locations.isEmpty()) {
            return locations.get(currentLocationIndex);
        }
        return null;
    }

    // Add dialogue methods
    public void interact() {
        SnailLocation currentLocation = locations.get(currentLocationIndex);
        String[] currentDialogues = currentLocation.dialogues();

        if (!showingDialog) {
            showingDialog = true;
            currentDialogue = 0;
        } else {
            currentDialogue++;
            if (currentDialogue >= currentDialogues.length) {
                closeDialog();
            }
        }
    }

    public void closeDialog() {
        showingDialog = false;
        currentDialogue = 0;
    }

    // Add new method for drawing dialogue box
    private void drawDialogBox(Graphics2D g2d) {
        int boxWidth = 300;
        int boxHeight = 80;
        int boxX = getCenterX() - boxWidth / 2;
        int boxY = getY() - boxHeight - 20;
        SnailLocation currentLocation = locations.get(currentLocationIndex);
        String[] currentDialogues = currentLocation.dialogues();

        // Draw dialogue box background
        g2d.setColor(DIALOG_BG_COLOR);
        g2d.fillRoundRect(boxX, boxY, boxWidth, boxHeight, 10, 10);
        g2d.setColor(Color.WHITE);
        g2d.drawRoundRect(boxX, boxY, boxWidth, boxHeight, 10, 10);

        // Draw text
        g2d.setFont(DIALOG_FONT);
        g2d.setColor(Color.WHITE);

        // This is the CORRECT line that uses the location-specific dialogue
        if (currentDialogue < currentDialogues.length) {
            drawWrappedText(g2d, currentDialogues[currentDialogue], boxX + 10, boxY + 30, boxWidth - 20);
        }

        g2d.setFont(DIALOG_PROMPT_FONT);
        g2d.drawString("Press E to continue", boxX + boxWidth - 100, boxY + boxHeight - 10);
    }

    // Add new method for drawing interaction prompt
    private void drawInteractionPrompt(Graphics2D g2d) {
        g2d.setColor(Color.WHITE);
        g2d.setFont(INTERACTION_FONT);
        String prompt = "Press E to talk";
        int promptWidth = g2d.getFontMetrics().stringWidth(prompt);
        g2d.drawString(prompt, getCenterX() - promptWidth / 2, getY() - 10);
    }

    // Helper method for text wrapping
    private void drawWrappedText(Graphics2D g2d, String text, int x, int y, int maxWidth) {
        FontMetrics fm = g2d.getFontMetrics();
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        int lineY = y;

        for (String word : words) {
            if (fm.stringWidth(line + word) < maxWidth) {
                line.append(word).append(" ");
            } else {
                g2d.drawString(line.toString(), x, lineY);
                line = new StringBuilder(word + " ");
                lineY += fm.getHeight();
            }
        }
        g2d.drawString(line.toString(), x, lineY);
    }

    public int getLocationsCount() {
        return locations != null ? locations.size() : 0;
    }
}