package com.buglife.states;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import com.buglife.main.GamePanel;
import com.buglife.main.GameStateManager;
import com.buglife.assets.SoundManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LevelCompleteState extends GameState {
    private static final Logger logger = LoggerFactory.getLogger(LevelCompleteState.class);
    private SoundManager soundManager;
    private static final Font BIG_FONT = new Font("Consolas", Font.BOLD, 80);
    private static final Font SMALL_FONT = new Font("Consolas", Font.PLAIN, 24);

    public LevelCompleteState(GameStateManager manager, SoundManager soundManager) {
        super(manager);
        this.soundManager = soundManager;
    }

    @Override
    public void init() {
        soundManager.stopAllSounds();
        soundManager.playSound("level_complete");
    }

    @Override
    public void update() {
        // No update logic needed
    }

    @Override
    public void draw(Graphics2D g) {
        // Black background
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, GamePanel.VIRTUAL_WIDTH, GamePanel.VIRTUAL_HEIGHT);

        // Draw "LEVEL COMPLETE" text
        g.setColor(Color.GREEN);
        g.setFont(BIG_FONT);
        String msg = "LEVEL COMPLETE";
        int msgWidth = g.getFontMetrics().stringWidth(msg);
        g.drawString(msg, (GamePanel.VIRTUAL_WIDTH - msgWidth) / 2, GamePanel.VIRTUAL_HEIGHT / 3);

        // Draw prompt to return to menu
        g.setColor(Color.WHITE);
        g.setFont(SMALL_FONT);
        String prompt = "Press Enter to return to the Main Menu";
        int promptWidth = g.getFontMetrics().stringWidth(prompt);
        g.drawString(prompt, (GamePanel.VIRTUAL_WIDTH - promptWidth) / 2, GamePanel.VIRTUAL_HEIGHT / 2);
    }

    @Override
    public void keyPressed(int keyCode) {
        if (keyCode == KeyEvent.VK_ENTER) {
            manager.setState(GameStateManager.MENU);
        }
    }

    @Override
    public void keyReleased(int keyCode) {
        // Not used
    }
}