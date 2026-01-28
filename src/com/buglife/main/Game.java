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
    }

    private void loadCustomFont() {
        try (InputStream is = getClass().getResourceAsStream("/res/fonts/Tiny5.ttf")) {
            if (is == null) {
                System.err.println("ERROR: Font file not found!");
                return;
            }

            Tiny5 = Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(12f);
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(Tiny5);
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
        final double timePerFrame = 1_000_000_000.0 / FPS;

        while (running) {
            long frameStart = System.nanoTime();

            // Update and render
            gamePanel.updateGame();
            gamePanel.repaint();
            Toolkit.getDefaultToolkit().sync();

            // Calculate and apply frame limiting
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
     * The main entry point for our application.
     */
    // In Game.java
    public void cleanup() {
        running = false;

        try {
            if (gameThread != null && gameThread.isAlive()) {
                gameThread.join(500);
                if (gameThread.isAlive()) {
                    System.err.println("Warning: Game thread did not stop cleanly.");
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

    public static void main(String[] args) {
        Game game = new Game();
        game.startGameThread();

        // Add shutdown hook for cleanup
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            game.cleanup();
        }));
    }
}