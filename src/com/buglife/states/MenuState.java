package com.buglife.states;

import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import com.buglife.assets.SoundManager;
import com.buglife.main.GameStateManager;
import com.buglife.save.SaveData;
import com.buglife.save.SaveManager;
import com.buglife.save.UserProfile;
import com.buglife.ui.MainMenu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MenuState extends GameState {
    private static final Logger logger = LoggerFactory.getLogger(MenuState.class);
    private MainMenu mainMenu;
    private SoundManager soundManager;

    public MenuState(GameStateManager manager, SoundManager soundManager) {
        super(manager);
        this.soundManager = soundManager;
        this.mainMenu = new MainMenu();
    }

    @Override
    public void init() {
        // Start menu music when entering menu state
        soundManager.stopAllSounds();
        soundManager.loopSound("menuMusic");
        // Refresh save status so "Continue" shows/hides correctly
        mainMenu.refreshSaveStatus();
    }

    @Override
    public void update() {
        // No specific update logic needed for menu
    }

    @Override
    public void draw(Graphics2D g) {
        // The menu draws itself
        mainMenu.draw(g);
    }

    @Override
    public void keyPressed(int keyCode) {
        if (keyCode == KeyEvent.VK_UP || keyCode == KeyEvent.VK_W) {
            mainMenu.moveUp();
            soundManager.playSound("menu");
        }

        if (keyCode == KeyEvent.VK_DOWN || keyCode == KeyEvent.VK_S) {
            mainMenu.moveDown();
            soundManager.playSound("menu");
        }

        if (keyCode == KeyEvent.VK_ENTER) {
            String selectedOption = mainMenu.options[mainMenu.currentSelection];

            if (selectedOption.equals("Continue")) {
                // Load the saved game
                String playerName = UserProfile.getActivePlayer();
                SaveData saveData = SaveManager.loadGame(playerName);
                if (saveData != null) {
                    soundManager.stopSound("menuMusic");
                    manager.getPlayingState().loadFromSave(saveData);
                    manager.setState(GameStateManager.PLAYING);
                    logger.info("Loaded save for {}: Level={}", playerName, saveData.getLevelId());
                } else {
                    logger.warn("No save found for {}, starting new game", playerName);
                    soundManager.stopSound("menuMusic");
                    manager.getPlayingState().restart();
                    manager.setState(GameStateManager.PLAYING);
                }
            } else if (selectedOption.equals("New Game")) {
                soundManager.stopSound("menuMusic");
                manager.getPlayingState().restart();
                manager.setState(GameStateManager.PLAYING);
            } else if (selectedOption.equals("Test Level")) {
                soundManager.stopSound("menuMusic");
                manager.getPlayingState().setLevel("level_test");
                manager.setState(GameStateManager.PLAYING);
            } else if (selectedOption.equals("Leaderboard")) {
                manager.setState(GameStateManager.LEADERBOARD);
            } else if (selectedOption.equals("Settings")) {
                manager.getSettingsState().setReturnState(GameStateManager.MENU);
                manager.setState(GameStateManager.SETTINGS);
            } else if (selectedOption.equals("Quit")) {
                System.exit(0);
            }
        }
    }

    @Override
    public void keyReleased(int keyCode) {
        // No key release logic needed for menu
    }

    @Override
    public void cleanup() {
        // Clean up any menu-specific resources if needed
    }
}