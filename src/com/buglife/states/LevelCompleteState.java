package com.buglife.states;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import com.buglife.main.GamePanel;
import com.buglife.main.GameStateManager;
import com.buglife.assets.SoundManager;

public class LevelCompleteState extends GameState {
    private SoundManager soundManager;
    private static final Font BIG_FONT = new Font("Consolas", Font.BOLD, 80);
    private static final Font SMALL_FONT = new Font("Consolas", Font.PLAIN, 24);
    private int selectedOption = 0;
    private String[] options;

    public LevelCompleteState(GameStateManager manager, SoundManager soundManager) {
        super(manager);
        this.soundManager = soundManager;
    }

    @Override
    public void init() {
        soundManager.stopAllSounds();
        soundManager.playSound("level_complete");
        
        // Check if this is the last level
        PlayingState playingState = manager.getPlayingState();
        if (playingState.isLastLevel()) {
            options = new String[]{"Main Menu"};
        } else {
            options = new String[]{"Next Level", "Main Menu"};
        }
        selectedOption = 0;
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

        // Draw menu options
        g.setFont(SMALL_FONT);
        int startY = GamePanel.VIRTUAL_HEIGHT / 2;
        for (int i = 0; i < options.length; i++) {
            if (i == selectedOption) {
                g.setColor(Color.YELLOW);
            } else {
                g.setColor(Color.WHITE);
            }
            String option = options[i];
            int optionWidth = g.getFontMetrics().stringWidth(option);
            g.drawString(option, (GamePanel.VIRTUAL_WIDTH - optionWidth) / 2, startY + (i * 40));
        }
    }

    @Override
    public void keyPressed(int keyCode) {
        if (keyCode == KeyEvent.VK_UP || keyCode == KeyEvent.VK_W) {
            selectedOption--;
            if (selectedOption < 0) selectedOption = options.length - 1;
            soundManager.playSound("menu");
        }
        
        if (keyCode == KeyEvent.VK_DOWN || keyCode == KeyEvent.VK_S) {
            selectedOption++;
            if (selectedOption >= options.length) selectedOption = 0;
            soundManager.playSound("menu");
        }
        
        if (keyCode == KeyEvent.VK_ENTER) {
            String selected = options[selectedOption];
            if (selected.equals("Next Level")) {
                manager.getPlayingState().goToNextLevel();
                manager.setState(GameStateManager.PLAYING);
            } else if (selected.equals("Main Menu")) {
                manager.setState(GameStateManager.MENU);
            }
        }
    }

    @Override
    public void keyReleased(int keyCode) {
        // Not used
    }
}