package com.buglife.main;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.io.IOException;
import java.io.InputStream;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.buglife.assets.SoundManager;
import com.buglife.config.ConfigManager;
import com.buglife.config.GameConstants;
import com.buglife.save.SaveManager;
import com.buglife.save.UserProfile;
import com.buglife.utils.PerformanceMonitor;

public class Game implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(Game.class);

    private JFrame window;
    private GamePanel gamePanel;
    private Thread gameThread;
    private final int FPS; // Target frames per second (loaded from config)
    public static Font Tiny5;
    private SoundManager soundManager;
    private ConfigManager configManager;
    volatile boolean running = false;

    public Game() {
        // 0. Initialize ConfigManager first
        configManager = ConfigManager.getInstance();
        GameConstants.initialize();
        FPS = configManager.getInt("game.targetFPS", 60);
        logger.info("Target FPS set to: {}", FPS);
        
        // 0.5. Ensure saves directory exists
        UserProfile.ensureSavesDirectory();

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
    }

    private void loadCustomFont() {
        try (InputStream is = getClass().getResourceAsStream("/res/fonts/Tiny5.ttf")) {
            if (is == null) {
                logger.error("Font file not found: /res/fonts/Tiny5.ttf");
                return;
            }

            Tiny5 = Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(12f);
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(Tiny5);
        } catch (IOException | FontFormatException e) {
            logger.error("Failed to load custom font", e);
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
     * Enhanced with frame timing tracking for debug overlay.
     */
    @Override
    public void run() {
        final double timePerFrame = 1_000_000_000.0 / FPS;
        PerformanceMonitor monitor = PerformanceMonitor.getInstance();

        while (running) {
            long frameStart = System.nanoTime();

            // ============================================================
            // UPDATE PHASE - Track timing
            // ============================================================
            long updateStart = System.nanoTime();
            gamePanel.updateGame();
            long updateEnd = System.nanoTime();
            double updateMs = (updateEnd - updateStart) / 1_000_000.0;

            // ============================================================
            // RENDER PHASE - Track timing
            // ============================================================
            long renderStart = System.nanoTime();
            gamePanel.paintImmediately(0, 0, gamePanel.getWidth(), gamePanel.getHeight());
            Toolkit.getDefaultToolkit().sync();
            long renderEnd = System.nanoTime();
            double renderMs = (renderEnd - renderStart) / 1_000_000.0;

            // ============================================================
            // PERFORMANCE MONITORING - Update metrics
            // ============================================================
            monitor.setFrameTiming(updateMs, renderMs);
            monitor.update();

            // ============================================================
            // FRAME LIMITING - Sleep to maintain target FPS
            // ============================================================
            long workTime = System.nanoTime() - frameStart;
            long sleepTime = (long) (timePerFrame - workTime);

            if (sleepTime > 0) {
                try {
                    long millis = sleepTime / 1_000_000;
                    int nanos = (int) (sleepTime % 1_000_000);
                    Thread.sleep(millis, nanos);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    /**
     * Cleanup and shutdown procedure.
     */
    public void cleanup() {
        running = false;

        // Shutdown save manager (cloud services)
        SaveManager.shutdown();

        try {
            if (gameThread != null && gameThread.isAlive()) {
                gameThread.join(500);
                if (gameThread.isAlive()) {
                    logger.warn("Game thread did not stop cleanly");
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        gameThread = null;

        if (soundManager != null) {
            soundManager.stopAllSounds();
        }

        if (window != null) {
            SwingUtilities.invokeLater(() -> window.dispose());
        }
    }

    /**
     * The main entry point for our application.
     */
    public static void main(String[] args) {
        Game game = new Game();
        game.startGameThread();

        // Add shutdown hook for cleanup
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            game.cleanup();
        }));
    }
}