package com.buglife.states;

import java.awt.Graphics2D;
import com.buglife.main.GameStateManager;

public abstract class GameState {
    protected GameStateManager manager;

    public GameState(GameStateManager manager) {
        this.manager = manager;
    }

    /**
     * Called when the state is entered.
     * Use this to initialize resources.
     */
    public abstract void init();

    /**
     * Called every frame to update game logic.
     */
    public abstract void update();

    /**
     * Called every frame to render graphics.
     */
    public abstract void draw(Graphics2D g);

    /**
     * Called when a key is pressed.
     */
    public abstract void keyPressed(int keyCode);

    /**
     * Called when a key is released.
     */
    public abstract void keyReleased(int keyCode);

    /**
     * Called when the state is exited.
     * Use this to clean up resources.
     */
    public void cleanup() {
        // Override if needed
    }
}