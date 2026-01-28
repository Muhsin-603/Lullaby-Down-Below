package com.buglife.states;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import com.buglife.assets.SoundManager;
import com.buglife.main.GamePanel;
import com.buglife.main.GameStateManager;

public class SettingsState extends GameState {
    private SoundManager soundManager;
    private static final Font TITLE_FONT = new Font("Consolas", Font.BOLD, 60);
    private static final Font OPTION_FONT = new Font("Consolas", Font.PLAIN, 24);
    private static final Font VALUE_FONT = new Font("Consolas", Font.BOLD, 24);
    
    private int selectedOption = 0;
    private final String[] options = {"Master Volume", "Music Volume", "SFX Volume", "Back"};
    
    private float masterVolume;
    private float musicVolume;
    private float sfxVolume;

    public SettingsState(GameStateManager manager, SoundManager soundManager) {
        super(manager);
        this.soundManager = soundManager;
    }

    @Override
    public void init() {
        // Load current volume settings
        masterVolume = soundManager.getMasterVolume();
        musicVolume = soundManager.getMusicVolume();
        sfxVolume = soundManager.getSFXVolume();
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

        // Draw title
        g.setColor(Color.WHITE);
        g.setFont(TITLE_FONT);
        String title = "SETTINGS";
        int titleWidth = g.getFontMetrics().stringWidth(title);
        g.drawString(title, (GamePanel.VIRTUAL_WIDTH - titleWidth) / 2, 150);

        // Draw options
        g.setFont(OPTION_FONT);
        int startY = 300;
        int spacing = 60;
        
        for (int i = 0; i < options.length; i++) {
            int y = startY + (i * spacing);
            
            // Highlight selected option
            if (i == selectedOption) {
                g.setColor(Color.YELLOW);
            } else {
                g.setColor(Color.WHITE);
            }
            
            String option = options[i];
            g.drawString(option, 300, y);
            
            // Draw volume bars for volume options
            if (i < 3) {
                float volume = 0;
                if (i == 0) volume = masterVolume;
                else if (i == 1) volume = musicVolume;
                else if (i == 2) volume = sfxVolume;
                
                drawVolumeBar(g, 600, y - 20, 400, 30, volume, i == selectedOption);
            }
        }
        
        // Draw instructions
        g.setColor(Color.GRAY);
        g.setFont(new Font("Consolas", Font.PLAIN, 16));
        g.drawString("Use Arrow Keys to navigate | Left/Right to adjust | Enter to go back", 200, 600);
    }
    
    private void drawVolumeBar(Graphics2D g, int x, int y, int width, int height, float volume, boolean selected) {
        // Background bar
        g.setColor(selected ? new Color(80, 80, 80) : new Color(50, 50, 50));
        g.fillRect(x, y, width, height);
        
        // Volume fill
        int fillWidth = (int) (width * volume);
        if (volume > 0.7f) {
            g.setColor(Color.GREEN);
        } else if (volume > 0.3f) {
            g.setColor(Color.YELLOW);
        } else {
            g.setColor(Color.RED);
        }
        g.fillRect(x, y, fillWidth, height);
        
        // Border
        g.setColor(selected ? Color.YELLOW : Color.WHITE);
        g.drawRect(x, y, width, height);
        
        // Percentage text
        g.setFont(VALUE_FONT);
        String percent = (int)(volume * 100) + "%";
        int percentWidth = g.getFontMetrics().stringWidth(percent);
        g.drawString(percent, x + width + 20, y + 22);
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
        
        // Adjust volumes with left/right arrows
        if (keyCode == KeyEvent.VK_LEFT || keyCode == KeyEvent.VK_A) {
            adjustVolume(-0.05f);
        }
        
        if (keyCode == KeyEvent.VK_RIGHT || keyCode == KeyEvent.VK_D) {
            adjustVolume(0.05f);
        }
        
        if (keyCode == KeyEvent.VK_ENTER) {
            if (selectedOption == 3) { // Back option
                manager.setState(GameStateManager.MENU);
            }
        }
        
        if (keyCode == KeyEvent.VK_ESCAPE) {
            manager.setState(GameStateManager.MENU);
        }
    }
    
    private void adjustVolume(float delta) {
        if (selectedOption == 0) {
            masterVolume = Math.max(0.0f, Math.min(1.0f, masterVolume + delta));
            soundManager.setMasterVolume(masterVolume);
        } else if (selectedOption == 1) {
            musicVolume = Math.max(0.0f, Math.min(1.0f, musicVolume + delta));
            soundManager.setMusicVolume(musicVolume);
        } else if (selectedOption == 2) {
            sfxVolume = Math.max(0.0f, Math.min(1.0f, sfxVolume + delta));
            soundManager.setSFXVolume(sfxVolume);
            soundManager.playSound("menu"); // Test SFX volume
        }
    }

    @Override
    public void keyReleased(int keyCode) {
        // Not used
    }
}
