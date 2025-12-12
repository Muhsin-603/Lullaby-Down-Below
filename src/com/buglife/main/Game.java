package src.com.buglife.main;

import javax.swing.*;

import src.com.buglife.assets.SoundManager;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;

public class Game implements Runnable {

    private JFrame window;
    private GamePanel gamePanel;
    private Thread gameThread;
    private final int FPS = 60; // Our target frames per second
    public static Font Tiny5;
    private SoundManager soundManager;
    volatile boolean running = false;

    public Game() {
        // 1. Load assets first
        loadCustomFont();
        soundManager = new SoundManager();
        soundManager.loadSound("music", "/res/sounds/game_theme.wav");
        soundManager.loopSound("menuMusic");

        // 2. Create the panel that will hold the game
        gamePanel = new GamePanel(soundManager);

        // 3. Create and configure the main window (the JFrame)
        window = new JFrame();
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setTitle("Lullaby Down Below");
        window.setUndecorated(true); // Remove title bar for fullscreen
        window.setResizable(false);
        
        // Add the panel to the window's content pane
        window.add(gamePanel);

        // 4. Set the window to fullscreen
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gd = ge.getDefaultScreenDevice();
        gd.setFullScreenWindow(window); // This also makes the window visible

        // 5. CRITICAL: Request keyboard focus AFTER the window is visible/fullscreen.
        // This is the most reliable way to ensure key presses are heard.
        gamePanel.requestFocusInWindow();

        System.out.println("Game initialized successfully in fullscreen mode.");
    }

    private void loadCustomFont() {
        try {
            // Get the font file from our resources folder
            InputStream is = getClass().getResourceAsStream("/res/fonts/Tiny5.ttf");
            if (is == null) {
                System.err.println("ERROR: Font file not found!");
                return;
            }

            // Create the font object
            Tiny5 = Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(12f); // Load with a base size (e.g., 12pt)

            // Register the font with the system's graphics environment
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(Tiny5);

            is.close(); // Close the stream
            System.out.println("Custom font loaded successfully!");

        } catch (IOException | FontFormatException e) {
            System.err.println("ERROR: Failed to load custom font!");
            e.printStackTrace();
        }
    }

    /**
     * Creates and starts the game thread. This is the "beating heart" of the game.
     */
    public void startGameThread() {
        running = true;
        gameThread = new Thread(this);
        gameThread.start(); // This will automatically call the run() method
    }

    /**
     * This is the Game Loop. It will run continuously.
     */
                @Override
    public void run() {
        double drawInterval = 1_000_000_000.0 / FPS;
        double delta = 0;
        long lastTime = System.nanoTime();
        long currentTime;

        while (running) {
            currentTime = System.nanoTime();
            delta += (currentTime - lastTime) / drawInterval;
            lastTime = currentTime;

            if (delta >= 1) {
                // 1. UPDATE: Update all game logic
                gamePanel.updateGame();

                // 2. DRAW: Use paintImmediately instead
                gamePanel.repaint();

                delta--;
            }
        }
    }

    /**
     * The main entry point for our application.
     */
    // In Game.java
    public void cleanup() {
        System.out.println("Cleanup requested..."); // Add more logging
        running = false; // Signal the game loop to stop

        // Wait for the game thread to actually finish
        try {
            if (gameThread != null && gameThread.isAlive()) {
                System.out.println("Waiting for game thread to stop...");
                gameThread.join(500); // Wait up to 500ms
                if (gameThread.isAlive()) {
                    System.err.println("Warning: Game thread did not stop cleanly.");
                    // Consider thread interruption if needed, but join is usually enough
                    // gameThread.interrupt();
                } else {
                    System.out.println("Game thread stopped.");
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore interrupt status
            System.err.println("Interrupted while waiting for game thread.");
        }
        gameThread = null; // Okay to set to null after join

        // Stop sounds
        if (soundManager != null) {
            System.out.println("Stopping sounds...");
            soundManager.stopAllSounds();
            // Consider adding a method to SoundManager to explicitly close Clips if
            // necessary
            // soundManager.closeAllClips();
        }

        // Dispose window
        if (window != null) {
            System.out.println("Disposing window...");
            // Ensure this runs on the EDT if called from shutdown hook
            SwingUtilities.invokeLater(() -> window.dispose());
            // window.dispose(); // This might cause issues if called from non-EDT thread
        }
        System.out.println("Cleanup finished.");
    }

    public static void main(String[] args) {
        Game game = new Game();
        game.startGameThread();

        // Add shutdown hook for cleanup
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            game.cleanup();
        }));
    }
}