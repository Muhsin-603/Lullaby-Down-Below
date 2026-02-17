package com.buglife.states;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import com.buglife.assets.SoundManager;
import com.buglife.main.Game;
import com.buglife.main.GamePanel;
import com.buglife.main.GameStateManager;
import com.buglife.save.CloudSaveManager;
import com.buglife.save.SaveData;
import com.buglife.save.SaveManager;
import com.buglife.save.UserProfile;
import com.buglife.utils.TelemetryClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * IdentifyState — The Crossroads.
 *
 * "IDENTIFY YOURSELF."
 *
 * The boot screen splits into two paths:
 *
 * Path A — "THE VETERAN" — A scrollable roster of all known players,
 *          fetched from the Overseer's /users/list endpoint. Click a name
 *          from the guest list and you're in instantly.
 *
 * Path B — "FRESH MEAT" — Press [N] / select "NEW VICTIM" to switch to
 *          the name-typing screen. Type a new name, press ENTER, and the
 *          Overseer registers a new soul.
 *
 * The Guest List is pre-fetched asynchronously when this screen initializes,
 * merging Overseer knowledge with local .profiles.json entries.
 */
public class IdentifyState extends GameState {
    private static final Logger logger = LoggerFactory.getLogger(IdentifyState.class);

    // ─── Mode enum ──────────────────────────────────────────────
    private enum Mode {
        ROSTER,     // Path A — picking from the guest list
        NEW_VICTIM  // Path B — typing a fresh name
    }

    private SoundManager soundManager;
    private Mode currentMode = Mode.ROSTER;

    // ─── Guest List (Roster) state ──────────────────────────────
    private List<String> guestList = new ArrayList<>();
    private int rosterSelection = 0;   // Currently highlighted name
    private int rosterScrollOffset = 0;
    private static final int VISIBLE_ROSTER_ROWS = 8;
    private boolean fetchInProgress = false;
    private boolean fetchComplete = false;
    private String fetchStatus = "CONTACTING THE OVERSEER...";

    // ─── New Victim (typing) state ──────────────────────────────
    private StringBuilder playerName = new StringBuilder();
    private static final int MAX_NAME_LENGTH = 12;

    // ─── Shared visual state ────────────────────────────────────
    private int cursorBlinkTimer = 0;
    private boolean showCursor = true;
    private String statusMessage = "";
    private Color statusColor = Color.WHITE;
    private boolean isProcessing = false;
    private int titleFlicker = 0;

    // ─── Fonts ──────────────────────────────────────────────────
    private Font titleFont;
    private Font promptFont;
    private Font nameFont;
    private Font statusFont;
    private Font instructionFont;
    private Font rosterFont;
    private Font rosterHighlightFont;

    public IdentifyState(GameStateManager manager, SoundManager soundManager) {
        super(manager);
        this.soundManager = soundManager;
    }

    @Override
    public void init() {
        currentMode = Mode.ROSTER;
        playerName.setLength(0);
        statusMessage = "";
        isProcessing = false;
        cursorBlinkTimer = 0;
        showCursor = true;
        rosterSelection = 0;
        rosterScrollOffset = 0;
        fetchComplete = false;
        fetchInProgress = false;
        fetchStatus = "CONTACTING THE OVERSEER...";
        guestList.clear();

        // Fonts — Tiny5 or Consolas fallback
        if (Game.Tiny5 != null) {
            titleFont          = Game.Tiny5.deriveFont(Font.BOLD, 52);
            promptFont         = Game.Tiny5.deriveFont(Font.PLAIN, 26);
            nameFont           = Game.Tiny5.deriveFont(Font.BOLD, 44);
            statusFont         = Game.Tiny5.deriveFont(Font.PLAIN, 22);
            instructionFont    = Game.Tiny5.deriveFont(Font.PLAIN, 16);
            rosterFont         = Game.Tiny5.deriveFont(Font.PLAIN, 30);
            rosterHighlightFont = Game.Tiny5.deriveFont(Font.BOLD, 32);
        } else {
            titleFont          = new Font("Consolas", Font.BOLD, 52);
            promptFont         = new Font("Consolas", Font.PLAIN, 26);
            nameFont           = new Font("Consolas", Font.BOLD, 44);
            statusFont         = new Font("Consolas", Font.PLAIN, 22);
            instructionFont    = new Font("Consolas", Font.PLAIN, 16);
            rosterFont         = new Font("Consolas", Font.PLAIN, 30);
            rosterHighlightFont = new Font("Consolas", Font.BOLD, 32);
        }

        // Pre-fetch the guest list asynchronously
        beginGuestListFetch();
    }

    /**
     * Kick off the async fetch of the guest list from the Overseer,
     * merging with locally-known players.
     */
    private void beginGuestListFetch() {
        fetchInProgress = true;
        fetchStatus = "CONTACTING THE OVERSEER...";

        new Thread(() -> {
            try {
                // Ask the Overseer for all known players (3 second timeout)
                List<String> overseerUsers = CloudSaveManager.fetchUserListWithTimeout(3);

                // Merge with locally known players (some may not be on the server)
                List<String> localPlayers = UserProfile.getKnownPlayers();
                List<String> merged = new ArrayList<>(overseerUsers);
                for (String local : localPlayers) {
                    if (!merged.contains(local)) {
                        merged.add(local);
                    }
                }

                // Sort alphabetically for clean arcade aesthetics
                merged.sort(String::compareToIgnoreCase);

                guestList = merged;

                if (guestList.isEmpty()) {
                    fetchStatus = "NO PLAYERS FOUND — BE THE FIRST";
                    // Auto-switch to new victim mode if list is empty
                    currentMode = Mode.NEW_VICTIM;
                } else {
                    fetchStatus = guestList.size() + " SOULS ON RECORD";
                }
                logger.info("Guest list loaded: {} players", guestList.size());
            } catch (Exception e) {
                logger.warn("Failed to fetch guest list: {}", e.getMessage());
                // Fallback to local-only
                guestList = new ArrayList<>(UserProfile.getKnownPlayers());
                guestList.sort(String::compareToIgnoreCase);
                if (guestList.isEmpty()) {
                    fetchStatus = "OVERSEER OFFLINE — TYPE YOUR NAME";
                    currentMode = Mode.NEW_VICTIM;
                } else {
                    fetchStatus = guestList.size() + " LOCAL SOULS FOUND";
                }
            } finally {
                fetchInProgress = false;
                fetchComplete = true;
            }
        }, "GuestListFetch").start();
    }

    @Override
    public void update() {
        cursorBlinkTimer++;
        if (cursorBlinkTimer >= 30) {
            cursorBlinkTimer = 0;
            showCursor = !showCursor;
        }
        titleFlicker++;
    }

    // ════════════════════════════════════════════════════════════
    //                       DRAWING
    // ════════════════════════════════════════════════════════════

    @Override
    public void draw(Graphics2D g) {
        int w = GamePanel.VIRTUAL_WIDTH;
        int h = GamePanel.VIRTUAL_HEIGHT;

        // === BLACK VOID ===
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, w, h);

        // === CRT Scanlines ===
        g.setColor(new Color(255, 255, 255, 8));
        for (int y = 0; y < h; y += 3) {
            g.drawLine(0, y, w, y);
        }

        // === TITLE ===
        drawTitle(g, w, h);

        // === Mode-specific content ===
        if (currentMode == Mode.ROSTER) {
            drawRosterMode(g, w, h);
        } else {
            drawNewVictimMode(g, w, h);
        }

        // === STATUS MESSAGE (shared) ===
        if (!statusMessage.isEmpty()) {
            g.setFont(statusFont);
            g.setColor(statusColor);
            int statusWidth = g.getFontMetrics().stringWidth(statusMessage);
            g.drawString(statusMessage, (w - statusWidth) / 2, h - 120);
        }
    }

    private void drawTitle(Graphics2D g, int w, int h) {
        g.setFont(titleFont);
        int greenPulse = 200 + (int)(55 * Math.sin(titleFlicker * 0.05));
        g.setColor(new Color(0, greenPulse, 0));
        String title = "IDENTIFY YOURSELF";
        int titleWidth = g.getFontMetrics().stringWidth(title);
        g.drawString(title, (w - titleWidth) / 2, 80);

        // Horizontal rule
        g.setColor(new Color(0, 150, 0, 100));
        g.drawLine(w / 4, 95, 3 * w / 4, 95);

        // Fetch status
        g.setFont(instructionFont);
        g.setColor(new Color(0, 140, 0, 200));
        int fsW = g.getFontMetrics().stringWidth(fetchStatus);
        g.drawString(fetchStatus, (w - fsW) / 2, 118);
    }

    // ─── Path A: The Roster ─────────────────────────────────────

    private void drawRosterMode(Graphics2D g, int w, int h) {
        // Prompt
        g.setFont(promptFont);
        g.setColor(new Color(0, 200, 0));
        String prompt = "SELECT YOUR NAME:";
        int promptWidth = g.getFontMetrics().stringWidth(prompt);
        g.drawString(prompt, (w - promptWidth) / 2, 160);

        if (guestList.isEmpty()) {
            // Still loading or empty
            g.setFont(statusFont);
            g.setColor(new Color(0, 150, 0, 150));
            String msg = fetchInProgress ? "LOADING..." : "NO NAMES ON FILE";
            int msgW = g.getFontMetrics().stringWidth(msg);
            g.drawString(msg, (w - msgW) / 2, h / 2);
        } else {
            // Draw scrollable roster
            int rosterX = w / 2 - 250;
            int rosterY = 190;
            int rowHeight = 48;
            int rosterWidth = 500;

            // Scroll indicators
            if (rosterScrollOffset > 0) {
                g.setFont(instructionFont);
                g.setColor(new Color(0, 180, 0));
                String up = "▲ MORE ▲";
                int upW = g.getFontMetrics().stringWidth(up);
                g.drawString(up, (w - upW) / 2, rosterY - 5);
            }

            int visibleEnd = Math.min(rosterScrollOffset + VISIBLE_ROSTER_ROWS, guestList.size());
            for (int i = rosterScrollOffset; i < visibleEnd; i++) {
                int drawY = rosterY + (i - rosterScrollOffset) * rowHeight;
                boolean isSelected = (i == rosterSelection);

                // Row background
                if (isSelected) {
                    g.setColor(new Color(0, 80, 0));
                    g.fillRect(rosterX, drawY, rosterWidth, rowHeight - 4);
                    g.setColor(new Color(0, 255, 0));
                    g.drawRect(rosterX, drawY, rosterWidth, rowHeight - 4);
                } else {
                    g.setColor(new Color(0, 25, 0));
                    g.fillRect(rosterX, drawY, rosterWidth, rowHeight - 4);
                    g.setColor(new Color(0, 80, 0));
                    g.drawRect(rosterX, drawY, rosterWidth, rowHeight - 4);
                }

                // Name text
                g.setFont(isSelected ? rosterHighlightFont : rosterFont);
                g.setColor(isSelected ? new Color(0, 255, 0) : new Color(0, 180, 0));
                String name = guestList.get(i);

                // Selection arrow
                String display = isSelected ? "> " + name : "  " + name;
                FontMetrics fm = g.getFontMetrics();
                g.drawString(display, rosterX + 15, drawY + fm.getAscent() + 5);
            }

            // Down arrow
            if (visibleEnd < guestList.size()) {
                g.setFont(instructionFont);
                g.setColor(new Color(0, 180, 0));
                String down = "▼ MORE ▼";
                int downW = g.getFontMetrics().stringWidth(down);
                int downY = rosterY + VISIBLE_ROSTER_ROWS * rowHeight + 10;
                g.drawString(down, (w - downW) / 2, downY);
            }
        }

        // ─── "NEW VICTIM" button at the bottom ─────────────────
        int btnW = 300;
        int btnH = 42;
        int btnX = (w - btnW) / 2;
        int btnY = h - 180;

        // Draw button
        boolean btnHighlight = (rosterSelection == -1); // -1 = NEW VICTIM focused
        g.setColor(btnHighlight ? new Color(80, 0, 0) : new Color(40, 0, 0));
        g.fillRect(btnX, btnY, btnW, btnH);
        g.setColor(btnHighlight ? new Color(255, 80, 80) : new Color(180, 60, 60));
        g.drawRect(btnX, btnY, btnW, btnH);

        g.setFont(promptFont);
        g.setColor(btnHighlight ? new Color(255, 100, 100) : new Color(200, 80, 80));
        String newBtn = "[ NEW VICTIM ]";
        int nbW = g.getFontMetrics().stringWidth(newBtn);
        g.drawString(newBtn, (w - nbW) / 2, btnY + 32);

        // Instructions
        g.setFont(instructionFont);
        g.setColor(new Color(0, 120, 0, 180));
        String instr = "[↑/↓] SCROLL   [ENTER] SELECT   [N] NEW VICTIM   [ESC] QUIT";
        int instrW = g.getFontMetrics().stringWidth(instr);
        g.drawString(instr, (w - instrW) / 2, h - 50);

        // Flavor
        g.setColor(new Color(0, 80, 0, 120));
        String flavor = "THE OVERSEER REMEMBERS ALL...";
        int flavorW = g.getFontMetrics().stringWidth(flavor);
        g.drawString(flavor, (w - flavorW) / 2, h - 25);
    }

    // ─── Path B: New Victim ─────────────────────────────────────

    private void drawNewVictimMode(Graphics2D g, int w, int h) {
        // Prompt
        g.setFont(promptFont);
        g.setColor(new Color(200, 80, 80));
        String prompt = "ENTER YOUR NAME, FRESH MEAT:";
        int promptWidth = g.getFontMetrics().stringWidth(prompt);
        g.drawString(prompt, (w - promptWidth) / 2, h / 2 - 80);

        // Input box
        int boxWidth = 500;
        int boxHeight = 60;
        int boxX = (w - boxWidth) / 2;
        int boxY = h / 2 - 50;

        g.setColor(new Color(30, 0, 0));
        g.fillRect(boxX, boxY, boxWidth, boxHeight);
        g.setColor(new Color(200, 60, 60));
        g.drawRect(boxX, boxY, boxWidth, boxHeight);

        // Typed name
        g.setFont(nameFont);
        g.setColor(new Color(255, 80, 80));
        String displayName = playerName.toString();
        int nameWidth = g.getFontMetrics().stringWidth(displayName);
        int nameX = (w - nameWidth) / 2;
        int nameY = boxY + 47;
        g.drawString(displayName, nameX, nameY);

        // Blinking cursor
        if (showCursor && !isProcessing && playerName.length() < MAX_NAME_LENGTH) {
            g.drawString("_", nameX + nameWidth + 2, nameY);
        }

        // Character count
        g.setFont(instructionFont);
        g.setColor(new Color(100, 40, 40));
        String charCount = playerName.length() + "/" + MAX_NAME_LENGTH;
        int countWidth = g.getFontMetrics().stringWidth(charCount);
        g.drawString(charCount, boxX + boxWidth - countWidth - 5, boxY + boxHeight + 18);

        // Instructions
        g.setFont(instructionFont);
        g.setColor(new Color(120, 60, 60, 200));
        String instr = "[TYPE]  [ENTER TO CONFIRM]  [BACKSPACE DELETE]  [ESC BACK TO ROSTER]";
        int instrW = g.getFontMetrics().stringWidth(instr);
        g.drawString(instr, (w - instrW) / 2, h - 50);

        // Flavor
        g.setColor(new Color(80, 0, 0, 120));
        String flavor = "ANOTHER SOUL FOR THE COLLECTION...";
        int flavorW = g.getFontMetrics().stringWidth(flavor);
        g.drawString(flavor, (w - flavorW) / 2, h - 25);
    }

    // ════════════════════════════════════════════════════════════
    //                      INPUT HANDLING
    // ════════════════════════════════════════════════════════════

    @Override
    public void keyPressed(int keyCode) {
        if (isProcessing) return;

        if (currentMode == Mode.ROSTER) {
            handleRosterInput(keyCode);
        } else {
            handleNewVictimInput(keyCode);
        }
    }

    // ─── Roster input ───────────────────────────────────────────

    private void handleRosterInput(int keyCode) {
        switch (keyCode) {
            case KeyEvent.VK_UP:
                if (rosterSelection == -1) {
                    // From NEW VICTIM button back to last roster item
                    rosterSelection = guestList.size() - 1;
                    ensureVisible(rosterSelection);
                } else if (rosterSelection > 0) {
                    rosterSelection--;
                    ensureVisible(rosterSelection);
                }
                statusMessage = "";
                break;

            case KeyEvent.VK_DOWN:
                if (rosterSelection >= 0 && rosterSelection < guestList.size() - 1) {
                    rosterSelection++;
                    ensureVisible(rosterSelection);
                } else if (rosterSelection == guestList.size() - 1) {
                    // Move focus to NEW VICTIM button
                    rosterSelection = -1;
                }
                statusMessage = "";
                break;

            case KeyEvent.VK_ENTER:
                if (rosterSelection == -1) {
                    // NEW VICTIM button selected
                    switchToNewVictim();
                } else if (rosterSelection >= 0 && rosterSelection < guestList.size()) {
                    // Veteran selected — lock them in
                    selectVeteran(guestList.get(rosterSelection));
                }
                break;

            case KeyEvent.VK_N:
                // Shortcut to switch to new victim mode
                switchToNewVictim();
                break;

            case KeyEvent.VK_ESCAPE:
                System.exit(0);
                break;
        }
    }

    private void ensureVisible(int index) {
        if (index < rosterScrollOffset) {
            rosterScrollOffset = index;
        } else if (index >= rosterScrollOffset + VISIBLE_ROSTER_ROWS) {
            rosterScrollOffset = index - VISIBLE_ROSTER_ROWS + 1;
        }
    }

    // ─── New Victim input ───────────────────────────────────────

    private void handleNewVictimInput(int keyCode) {
        // ENTER — Submit the name
        if (keyCode == KeyEvent.VK_ENTER) {
            if (playerName.length() == 0) {
                statusMessage = "NAME CANNOT BE EMPTY";
                statusColor = new Color(255, 80, 80);
                return;
            }
            processNewVictim();
            return;
        }

        // BACKSPACE
        if (keyCode == KeyEvent.VK_BACK_SPACE) {
            if (playerName.length() > 0) {
                playerName.deleteCharAt(playerName.length() - 1);
                statusMessage = "";
            }
            return;
        }

        // ESCAPE — Back to roster (if there are names to show)
        if (keyCode == KeyEvent.VK_ESCAPE) {
            if (!guestList.isEmpty()) {
                currentMode = Mode.ROSTER;
                statusMessage = "";
                rosterSelection = 0;
                rosterScrollOffset = 0;
            } else {
                System.exit(0);
            }
            return;
        }

        // Letter/number/underscore/dash input
        char c = (char) keyCode;
        if (playerName.length() < MAX_NAME_LENGTH) {
            if (Character.isLetterOrDigit(c) || c == '_' || c == '-') {
                playerName.append(Character.toUpperCase(c));
                statusMessage = "";
            }
        } else {
            statusMessage = "MAX LENGTH REACHED";
            statusColor = Color.YELLOW;
        }
    }

    @Override
    public void keyReleased(int keyCode) {
        // Not used
    }

    // ════════════════════════════════════════════════════════════
    //               IDENTIFICATION PROCESSING
    // ════════════════════════════════════════════════════════════

    /**
     * Path A — The Veteran. An existing name was selected from the roster.
     * No registration needed, just lock them in and go.
     */
    private void selectVeteran(String name) {
        isProcessing = true;
        String uppedName = name.toUpperCase();

        statusMessage = "WELCOME BACK, " + uppedName + "!";
        statusColor = new Color(0, 255, 0);
        logger.info("Returning veteran selected from roster: {}", uppedName);

        // Make sure they're in the local registry
        if (!UserProfile.isKnownLocally(uppedName)) {
            UserProfile.registerLocally(uppedName);
        }

        // Lock name into the session
        UserProfile.setActivePlayer(uppedName);

        // Fetch their total playtime from the save file
        SaveData save = SaveManager.loadGame(uppedName);
        long startingTotalPlaytime = (save != null) ? save.getTotalPlaytimeSeconds() : 0;

        // Re-init TelemetryClient with this player and their career total
        TelemetryClient.shutdown();
        TelemetryClient.initialize(uppedName, startingTotalPlaytime);

        // Brief pause to show welcome, then menu
        new Thread(() -> {
            try {
                Thread.sleep(1200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            manager.setState(GameStateManager.MENU);
        }, "VeteranTransition").start();
    }

    /**
     * Path B — Fresh Meat. A new name was typed in.
     * Register with both local brain and the Overseer.
     */
    private void processNewVictim() {
        isProcessing = true;
        String name = playerName.toString().toUpperCase();

        // Check if this name already exists
        if (guestList.contains(name) || UserProfile.isKnownLocally(name)) {
            statusMessage = "NAME ALREADY TAKEN — USE THE ROSTER";
            statusColor = new Color(255, 200, 80);
            isProcessing = false;
            return;
        }

        statusMessage = "REGISTERING NEW SOUL: " + name;
        statusColor = new Color(100, 200, 255);
        logger.info("New victim registered: {}", name);

        // Register locally
        UserProfile.registerLocally(name);

        // Register with the Overseer (async)
        CloudSaveManager.registerWithOverseerAsync(name);

        // Lock name into the session
        UserProfile.setActivePlayer(name);

        // Re-init TelemetryClient (starts at 0 playtime for a fresh soul)
        TelemetryClient.shutdown();
        TelemetryClient.initialize(name, 0);

        // Transition
        new Thread(() -> {
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            manager.setState(GameStateManager.MENU);
        }, "NewVictimTransition").start();
    }

    /**
     * Switch to the new victim typing mode.
     */
    private void switchToNewVictim() {
        currentMode = Mode.NEW_VICTIM;
        playerName.setLength(0);
        statusMessage = "";
    }

    @Override
    public void cleanup() {
        // Nothing to clean up
    }
}
