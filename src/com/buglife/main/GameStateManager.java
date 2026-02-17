package com.buglife.main;

import java.awt.Graphics2D;
import com.buglife.assets.SoundManager;
import com.buglife.states.GameState;
import com.buglife.states.IdentifyState;
import com.buglife.states.LeaderboardState;
import com.buglife.states.MenuState;
import com.buglife.states.PlayingState;
import com.buglife.states.SettingsState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.buglife.states.GameOverState;
import com.buglife.states.LevelCompleteState;

public class GameStateManager {
    private static final Logger logger = LoggerFactory.getLogger(GameStateManager.class);
    // State constants
    public static final int MENU = 0;
    public static final int PLAYING = 1;
    public static final int PAUSED = 2;
    public static final int GAME_OVER = 3;
    public static final int LEVEL_COMPLETE = 4;
    public static final int SETTINGS = 5;
    public static final int IDENTIFY = 6;
    public static final int LEADERBOARD = 7;

    private GameState currentState;
    private int nextStateID = -1;
    private SoundManager soundManager;
    private GamePanel gamePanel; // Reference to parent panel for context

    // Cache states to avoid repeated instantiation
    private IdentifyState identifyState;
    private MenuState menuState;
    private PlayingState playingState;

    private GameOverState gameOverState;
    private SettingsState settingsState;
    private LevelCompleteState levelCompleteState;
    private LeaderboardState leaderboardState;

    public GameStateManager(SoundManager soundManager, GamePanel gamePanel) {
        this.soundManager = soundManager;
        this.gamePanel = gamePanel;

        // Pre-create states
        this.identifyState = new IdentifyState(this, soundManager);
        this.menuState = new MenuState(this, soundManager);
        this.playingState = new PlayingState(this, soundManager);

        //game over and won game state
        this.gameOverState = new GameOverState(this, soundManager);
        this.settingsState = new SettingsState(this, soundManager);
        this.levelCompleteState = new LevelCompleteState(this, soundManager);
        this.leaderboardState = new LeaderboardState(this, soundManager);

        // Start with identify screen (the gatekeeper)
        setState(IDENTIFY);
    }

    public PlayingState getPlayingState() {
        return this.playingState;
    }

    /**
     * Change to a new game state.
     */
    public void setState(int stateID) {
        nextStateID = stateID; // Instead of changing immediately, just set the request
    }
    private void applyStateChange() {
        if (nextStateID == -1) {
            return; // No change requested
        }

        // Cleanup the old state
        if (currentState != null) {
            currentState.cleanup();
        }

        // Set the new state based on the ID
        switch (nextStateID) {
            case MENU:
                currentState = menuState;
                break;
            case PLAYING:
                currentState = playingState;
                break;
            // --- ADD CASES FOR NEW STATES ---
            case GAME_OVER:
                currentState = gameOverState;
                break;
            case LEVEL_COMPLETE:
                currentState = levelCompleteState;
                break;
            case SETTINGS:
                currentState = settingsState;
                break;
            case IDENTIFY:
                currentState = identifyState;
                break;
            case LEADERBOARD:
                currentState = leaderboardState;
                break;
            default:
                logger.error("Unknown state ID: {}", nextStateID);
        }

        nextStateID = -1; // Reset the request

        // Initialize the new state
        if (currentState != null) {
            currentState.init();
        }
    }

    /**
     * Update the current state.
     */
    public void update() {
        // Apply any pending state changes at the beginning of the update loop
        applyStateChange();

        if (currentState != null) {
            currentState.update();
        }
    }

    /**
     * Render the current state.
     */
    public void draw(Graphics2D g) {
        if (currentState != null) {
            currentState.draw(g);
        }
    }

    /**
     * Handle key press events.
     */
    public void keyPressed(int keyCode) {
        if (currentState != null) {
            currentState.keyPressed(keyCode);
        }
    }

    /**
     * Handle key release events.
     */
    public void keyReleased(int keyCode) {
        if (currentState != null) {
            currentState.keyReleased(keyCode);
        }
    }

    /**
     * Get the SettingsState instance.
     */
    public SettingsState getSettingsState() {
        return this.settingsState;
    }

    /**
     * Get the SoundManager instance.
     */
    public SoundManager getSoundManager() {
        return soundManager;
    }

    /**
     * Get the GamePanel instance for context.
     */
    public GamePanel getGamePanel() {
        return gamePanel;
    }
}