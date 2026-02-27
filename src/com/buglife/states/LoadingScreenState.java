package com.buglife.states;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import com.buglife.main.GameStateManager;
import com.buglife.assets.SoundManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoadingScreenState extends GameState {
    private static final Logger logger = LoggerFactory.getLogger(LoadingScreenState.class);
    private SoundManager soundManager;
    private static final Font BIG_FONT = new Font("Consolas", Font.BOLD, 48);
    private static final Font SMALL_FONT = new Font("Consolas", Font.PLAIN, 24);
    
    private boolean isLoadingComplete = false;
    private int loadingProgress = 0;
    private long startTime;
    private static final int MIN_LOADING_TIME = 2000; // Minimum 2 seconds for loading screen
    
    public LoadingScreenState(GameStateManager manager, SoundManager soundManager) {
        super(manager);
        this.soundManager = soundManager;
    }

    @Override
    public void init() {
        soundManager.stopAllSounds();
        soundManager.playSound("menu_selection");
        
        isLoadingComplete = false;
        loadingProgress = 0;
        startTime = System.currentTimeMillis();
        
        logger.info("Loading screen initialized");
    }

    @Override
    public void update() {
        // Simulate loading progress
        if (!isLoadingComplete) {
            loadingProgress += 10;
            if (loadingProgress >= 100) {
                loadingProgress = 100;
                // Check if minimum loading time has passed
                long elapsedTime = System.currentTimeMillis() - startTime;
                if (elapsedTime >= MIN_LOADING_TIME) {
                    isLoadingComplete = true;
                    // Transition to playing state
                    manager.getPlayingState().goToNextLevel();
                    manager.setState(GameStateManager.PLAYING);
                }
            }
        }
    }

    @Override
    public void draw(Graphics2D g) {
        // Black background
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, GamePanel.VIRTUAL_WIDTH, GamePanel.VIRTUAL_HEIGHT);

        // Draw loading title
        g.setColor(Color.WHITE);
        g.setFont(BIG_FONT);
        String title = "LOADING";
        int titleWidth = g.getFontMetrics().stringWidth(title);
        g.drawString(title, (GamePanel.VIRTUAL_WIDTH - titleWidth) / 2, GamePanel.VIRTUAL_HEIGHT / 3);

        // Draw loading animation
        g.setColor(Color.GREEN);
        int barWidth = 400;
        int barHeight = 30;
        int barX = (GamePanel.VIRTUAL_WIDTH - barWidth) / 2;
        int barY = GamePanel.VIRTUAL_HEIGHT / 2;
        
        // Draw progress bar background
        g.setColor(new Color(50, 50, 50));
        g.fillRect(barX, barY, barWidth, barHeight);
        
        // Draw progress bar fill
        g.setColor(Color.GREEN);
        int fillWidth = (int) ((loadingProgress / 100.0) * barWidth);
        g.fillRect(barX, barY, fillWidth, barHeight);
        
        // Draw progress percentage
        g.setColor(Color.WHITE);
        g.setFont(SMALL_FONT);
        String progressText = loadingProgress + "%";
        int textWidth = g.getFontMetrics().stringWidth(progressText);
        g.drawString(progressText, (GamePanel.VIRTUAL_WIDTH - textWidth) / 2, barY + barHeight + 40);

        // Draw loading dots animation
        g.setColor(Color.WHITE);
        int dotX = (GamePanel.VIRTUAL_WIDTH - 60) / 2;
        int dotY = barY + barHeight + 80;
        int dotSize = 10;
        
        long time = System.currentTimeMillis() / 300;
        for (int i = 0; i < 3; i++) {
            if (i == time % 3) {
                g.fillOval(dotX + (i * 20), dotY, dotSize + 5, dotSize + 5);
            } else {
                g.fillOval(dotX + (i * 20), dotY, dotSize, dotSize);
            }
        }
    }

    @Override
    public void keyPressed(int keyCode) {
        // Ignore key presses during loading
    }

    @Override
    public void keyReleased(int keyCode) {
        // Ignore key releases during loading
    }

    @Override
    public void cleanup() {
        logger.info("Loading screen cleanup completed");
    }
}