package com.buglife.states;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import com.buglife.main.GameStateManager;

/**
 * LoadingState - Displays an atmospheric loading screen between levels.
 *
 * Resolves Issue #29: Loading screen Between levels
 *
 * This state is entered when a level is completed and the next level
 * needs to be loaded. It shows a progress bar, level title, and
 * a fade-in/fade-out animation to make transitions feel professional.
 *
 * Flow: PlayingState (level complete) → LoadingState → PlayingState (next level)
 */
public class LoadingState extends GameState {

    // ─── Constants ───────────────────────────────────────────────
    private static final int    SCREEN_WIDTH      = 1366;
    private static final int    SCREEN_HEIGHT     = 768;
    private static final float  FADE_SPEED        = 0.02f;   // fade per frame
    private static final long   MIN_DISPLAY_MS    = 2000;    // show at least 2 seconds
    private static final Color  BG_COLOR          = new Color(5, 5, 10);      // near-black
    private static final Color  BAR_BG_COLOR      = new Color(30, 30, 40);    // dark grey
    private static final Color  BAR_FILL_COLOR    = new Color(180, 100, 220); // eerie purple
    private static final Color  TEXT_COLOR        = new Color(220, 200, 255); // soft lavender
    private static final Color  SUBTITLE_COLOR    = new Color(130, 110, 160); // dim purple
    private static final String FONT_NAME         = "Monospaced";

    // ─── State machine for the loading screen itself ──────────────
    public enum Phase {
        FADE_IN,      // screen fades from black into loading screen
        LOADING,      // progress bar fills up
        FADE_OUT      // screen fades back to black before next level
    }

    // ─── Fields ──────────────────────────────────────────────────
    private Phase   phase          = Phase.FADE_IN;
    private float   alpha          = 0.0f;     // 0 = transparent, 1 = opaque
    private float   progress       = 0.0f;     // 0.0 to 1.0 for progress bar
    private long    loadStartTime  = 0;
    private boolean isFinished     = false;

    private String  nextLevelId;
    private String  nextLevelName;
    private PlayingState playingState;

    // ─── Tip messages shown while loading ────────────────────────
    private static final String[] TIPS = {
        "The shadows are your only weapon...",
        "If you cry, they will hear you.",
        "Watch your hunger. Silence costs nothing.",
        "The snail knows the way. Trust it.",
        "Every web can be escaped... if you're fast enough."
    };
    private int tipIndex = 0;

    // ─────────────────────────────────────────────────────────────
    //  Constructor
    // ─────────────────────────────────────────────────────────────

    /**
     * Create a new LoadingState.
     *
     * @param manager         Game State Manager
     * @param nextLevelId     Level ID to load (e.g., "level2")
     * @param nextLevelName   Human-readable name (e.g., "Level 2")
     * @param playingState    Reference to PlayingState for callback
     */
    public LoadingState(GameStateManager manager, String nextLevelId, String nextLevelName, PlayingState playingState) {
        super(manager);
        this.nextLevelId = nextLevelId;
        this.nextLevelName = nextLevelName;
        this.playingState = playingState;
        this.loadStartTime = System.currentTimeMillis();
        // pick a random tip each time
        this.tipIndex = (int)(Math.random() * TIPS.length);
    }

    // ─────────────────────────────────────────────────────────────
    //  Update  (called once per game tick / ~60 FPS)
    // ─────────────────────────────────────────────────────────────
    @Override
    public void update() {
        switch (phase) {

            case FADE_IN:
                alpha = Math.min(1.0f, alpha + FADE_SPEED);
                if (alpha >= 1.0f) {
                    phase = Phase.LOADING;
                }
                break;

            case LOADING:
                // Simulate loading progress (in real usage, tie this to actual tile loading)
                long elapsed = System.currentTimeMillis() - loadStartTime;
                progress = Math.min(1.0f, (float) elapsed / MIN_DISPLAY_MS);

                if (progress >= 1.0f) {
                    phase = Phase.FADE_OUT;
                }
                break;

            case FADE_OUT:
                alpha = Math.max(0.0f, alpha - FADE_SPEED);
                if (alpha <= 0.0f) {
                    isFinished = true;
                    // Tell PlayingState to actually load the level
                    playingState.loadLevel(nextLevelId);
                    // Switch back to playing state
                    manager.setState(GameStateManager.PLAYING);
                }
                break;
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  Render  (called by the game's paint/render method)
    // ─────────────────────────────────────────────────────────────
    @Override
    public void draw(Graphics2D g) {
        // Enable anti-aliasing for smooth text
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Apply fade alpha to entire screen
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

        // ── Background ──────────────────────────────────────────
        g.setColor(BG_COLOR);
        g.fillRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);

        // ── Subtle scanline texture (horror atmosphere) ──────────
        drawScanlines(g);

        // ── Title: "DESCENDING" ─────────────────────────────────────
        g.setFont(new Font(FONT_NAME, Font.BOLD, 13));
        g.setColor(SUBTITLE_COLOR);
        drawCentered(g, "[ DESCENDING ]", SCREEN_HEIGHT / 2 - 80);

        // ── Level Name ───────────────────────────────────────────
        g.setFont(new Font(FONT_NAME, Font.BOLD, 32));
        g.setColor(TEXT_COLOR);
        drawCentered(g, nextLevelName.toUpperCase(), SCREEN_HEIGHT / 2 - 40);

        // ── Progress Bar ─────────────────────────────────────────
        drawProgressBar(g);

        // ── Tip / Lore text ──────────────────────────────────────
        g.setFont(new Font(FONT_NAME, Font.ITALIC, 11));
        g.setColor(SUBTITLE_COLOR);
        drawCentered(g, "\"" + TIPS[tipIndex] + "\"", SCREEN_HEIGHT / 2 + 80);

        // Reset composite so other states render normally
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
    }

    // ─────────────────────────────────────────────────────────────
    //  Private helpers
    // ─────────────────────────────────────────────────────────────

    /** Draws the animated progress bar */
    private void drawProgressBar(Graphics2D g) {
        int barWidth  = 400;
        int barHeight = 8;
        int barX      = (SCREEN_WIDTH  - barWidth)  / 2;
        int barY      = (SCREEN_HEIGHT / 2) + 20;

        // background track
        g.setColor(BAR_BG_COLOR);
        g.fillRoundRect(barX, barY, barWidth, barHeight, 6, 6);

        // filled portion
        int filled = (int)(barWidth * progress);
        if (filled > 0) {
            g.setColor(BAR_FILL_COLOR);
            g.fillRoundRect(barX, barY, filled, barHeight, 6, 6);
        }

        // percentage text
        g.setFont(new Font(FONT_NAME, Font.PLAIN, 10));
        g.setColor(SUBTITLE_COLOR);
        String pct = (int)(progress * 100) + "%";
        drawCentered(g, pct, barY + 22);
    }

    /** Draws subtle horizontal scanlines for atmosphere */
    private void drawScanlines(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 18));
        for (int y = 0; y < SCREEN_HEIGHT; y += 3) {
            g.drawLine(0, y, SCREEN_WIDTH, y);
        }
    }

    /** Draws a string horizontally centered at a given Y position */
    private void drawCentered(Graphics2D g, String text, int y) {
        FontMetrics fm = g.getFontMetrics();
        int x = (SCREEN_WIDTH - fm.stringWidth(text)) / 2;
        g.drawString(text, x, y);
    }

    // ─────────────────────────────────────────────────────────────
    //  Public API
    // ─────────────────────────────────────────────────────────────

    /** Returns true when loading is done and next level should start */
    public boolean isFinished() {
        return isFinished;
    }

    @Override
    public void init() {
        // No initialization needed for loading screen
    }

    @Override
    public void keyPressed(int keyCode) {
        // No input handling during loading screen
    }

    @Override
    public void keyReleased(int keyCode) {
        // No input handling during loading screen
    }
}