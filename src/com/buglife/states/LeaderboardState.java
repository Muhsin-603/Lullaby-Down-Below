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
import com.buglife.save.CloudSaveManager.LeaderboardEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LeaderboardState — The Hall of Infamy.
 *
 * A retro arcade leaderboard screen that fetches the top 10 players
 * from the Overseer's /leaderboard endpoint. Sorted by fewest deaths
 * (because in this game, dying less is the flex).
 *
 * The Overseer does the sorting — Java just draws.
 *
 * Big retro font, CRT scanlines, green-on-black. Classic arcade high
 * score table energy.
 *
 *   RANK   NAME          DEATHS
 *    1.    DRA              0
 *    2.    BUGMAN           3
 *    3.    SPIDER_KID      12
 */
public class LeaderboardState extends GameState {
    private static final Logger logger = LoggerFactory.getLogger(LeaderboardState.class);

    private SoundManager soundManager;

    // ─── Data ───────────────────────────────────────────────────
    private List<LeaderboardEntry> entries = new ArrayList<>();
    private boolean fetchInProgress = false;
    private boolean fetchComplete = false;
    private String fetchStatus = "CONTACTING THE OVERSEER...";

    // ─── Categories ─────────────────────────────────────────────
    private static final String[] CATEGORIES = {"deaths", "playtime", "levels_completed"};
    private static final String[] CATEGORY_LABELS = {"DEATHS", "PLAYTIME", "LEVELS CLEARED"};
    private int selectedCategory = 0;

    // ─── Visual ─────────────────────────────────────────────────
    private int flickerTimer = 0;
    private int glowPulse = 0;

    // ─── Fonts ──────────────────────────────────────────────────
    private Font titleFont;
    private Font headerFont;
    private Font entryFont;
    private Font rankFont;
    private Font statusFont;
    private Font instructionFont;

    public LeaderboardState(GameStateManager manager, SoundManager soundManager) {
        super(manager);
        this.soundManager = soundManager;
    }

    @Override
    public void init() {
        entries.clear();
        fetchInProgress = false;
        fetchComplete = false;
        fetchStatus = "CONTACTING THE OVERSEER...";
        selectedCategory = 0;
        flickerTimer = 0;
        glowPulse = 0;

        // Fonts
        if (Game.Tiny5 != null) {
            titleFont       = Game.Tiny5.deriveFont(Font.BOLD, 56);
            headerFont      = Game.Tiny5.deriveFont(Font.BOLD, 28);
            entryFont       = Game.Tiny5.deriveFont(Font.PLAIN, 30);
            rankFont        = Game.Tiny5.deriveFont(Font.BOLD, 34);
            statusFont      = Game.Tiny5.deriveFont(Font.PLAIN, 22);
            instructionFont = Game.Tiny5.deriveFont(Font.PLAIN, 16);
        } else {
            titleFont       = new Font("Consolas", Font.BOLD, 56);
            headerFont      = new Font("Consolas", Font.BOLD, 28);
            entryFont       = new Font("Consolas", Font.PLAIN, 30);
            rankFont        = new Font("Consolas", Font.BOLD, 34);
            statusFont      = new Font("Consolas", Font.PLAIN, 22);
            instructionFont = new Font("Consolas", Font.PLAIN, 16);
        }

        // Fetch leaderboard data
        fetchLeaderboard();
    }

    private void fetchLeaderboard() {
        fetchInProgress = true;
        fetchComplete = false;
        fetchStatus = "CONTACTING THE OVERSEER...";
        entries.clear();

        new Thread(() -> {
            try {
                String category = CATEGORIES[selectedCategory];
                List<LeaderboardEntry> result =
                        CloudSaveManager.fetchLeaderboardWithTimeout(category, 10, 4);
                entries = result;

                if (entries.isEmpty()) {
                    fetchStatus = "NO DATA — BE THE FIRST TO MAKE HISTORY";
                } else {
                    fetchStatus = entries.size() + " ENTRIES FOUND";
                }
                logger.info("Leaderboard fetched: {} entries for '{}'", entries.size(), category);
            } catch (Exception e) {
                logger.warn("Failed to fetch leaderboard: {}", e.getMessage());
                fetchStatus = "OVERSEER UNREACHABLE";
            } finally {
                fetchInProgress = false;
                fetchComplete = true;
            }
        }, "LeaderboardFetch").start();
    }

    @Override
    public void update() {
        flickerTimer++;
        glowPulse++;
    }

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

        // === TITLE: "HALL OF INFAMY" ===
        g.setFont(titleFont);
        int pulse = 180 + (int)(75 * Math.sin(glowPulse * 0.04));
        g.setColor(new Color(pulse, pulse / 3, 0)); // amber/orange glow
        String title = "HALL OF INFAMY";
        int titleWidth = g.getFontMetrics().stringWidth(title);
        g.drawString(title, (w - titleWidth) / 2, 75);

        // Horizontal rule
        g.setColor(new Color(180, 80, 0, 120));
        g.drawLine(w / 4, 90, 3 * w / 4, 90);

        // === Category selector ===
        g.setFont(headerFont);
        String catLabel = "< " + CATEGORY_LABELS[selectedCategory] + " >";
        int catW = g.getFontMetrics().stringWidth(catLabel);
        g.setColor(new Color(255, 200, 80));
        g.drawString(catLabel, (w - catW) / 2, 125);

        // Fetch status
        g.setFont(instructionFont);
        g.setColor(new Color(150, 100, 0, 180));
        int fsW = g.getFontMetrics().stringWidth(fetchStatus);
        g.drawString(fetchStatus, (w - fsW) / 2, 148);

        // === Leaderboard table ===
        if (fetchInProgress) {
            g.setFont(statusFont);
            g.setColor(new Color(255, 200, 80, 150));
            String loading = "LOADING...";
            int lw = g.getFontMetrics().stringWidth(loading);
            g.drawString(loading, (w - lw) / 2, h / 2);
        } else if (entries.isEmpty()) {
            g.setFont(statusFont);
            g.setColor(new Color(150, 100, 50, 180));
            String empty = "- EMPTY -";
            int ew = g.getFontMetrics().stringWidth(empty);
            g.drawString(empty, (w - ew) / 2, h / 2);
        } else {
            drawTable(g, w, h);
        }

        // === Instructions ===
        g.setFont(instructionFont);
        g.setColor(new Color(150, 100, 0, 200));
        String instr = "[←/→] CATEGORY   [R] REFRESH   [ESC/ENTER] BACK TO MENU";
        int instrW = g.getFontMetrics().stringWidth(instr);
        g.drawString(instr, (w - instrW) / 2, h - 50);

        // Flavor
        g.setColor(new Color(80, 40, 0, 120));
        String flavor = "THE OVERSEER SEES ALL. THE OVERSEER JUDGES ALL.";
        int flavorW = g.getFontMetrics().stringWidth(flavor);
        g.drawString(flavor, (w - flavorW) / 2, h - 25);
    }

    private void drawTable(Graphics2D g, int w, int h) {
        // Column positions
        int tableX = w / 2 - 350;
        int colRank = tableX;
        int colName = tableX + 100;
        int colValue = tableX + 550;
        int startY = 185;
        int rowHeight = 46;

        // Header
        g.setFont(headerFont);
        g.setColor(new Color(255, 200, 80));
        g.drawString("RANK", colRank, startY);
        g.drawString("NAME", colName, startY);

        String valueHeader = CATEGORY_LABELS[selectedCategory];
        FontMetrics hfm = g.getFontMetrics();
        int vhW = hfm.stringWidth(valueHeader);
        g.drawString(valueHeader, colValue - vhW + 100, startY);

        // Header underline
        g.setColor(new Color(180, 100, 0, 80));
        g.drawLine(tableX, startY + 8, tableX + 700, startY + 8);

        // Entries
        for (int i = 0; i < entries.size() && i < 10; i++) {
            LeaderboardEntry entry = entries.get(i);
            int drawY = startY + (i + 1) * rowHeight;

            // Podium colors for top 3
            Color rowColor;
            if (i == 0) {
                rowColor = new Color(255, 215, 0);  // GOLD
            } else if (i == 1) {
                rowColor = new Color(192, 192, 192); // SILVER
            } else if (i == 2) {
                rowColor = new Color(205, 127, 50);  // BRONZE
            } else {
                rowColor = new Color(0, 200, 0);     // Green for the rest
            }

            // Subtle row background for top 3
            if (i < 3) {
                g.setColor(new Color(rowColor.getRed(), rowColor.getGreen(), rowColor.getBlue(), 15));
                g.fillRect(tableX - 10, drawY - rowHeight + 14, 720, rowHeight - 4);
            }

            // Rank
            g.setFont(rankFont);
            g.setColor(rowColor);
            String rankStr = String.valueOf(entry.getRank()) + ".";
            g.drawString(rankStr, colRank + 20, drawY);

            // Name
            g.setFont(entryFont);
            g.setColor(rowColor);
            g.drawString(entry.getPlayerName(), colName, drawY);

            // Value
            String valueStr;
            if (CATEGORIES[selectedCategory].equals("playtime")) {
                valueStr = formatPlaytime(entry.getValue());
            } else {
                valueStr = String.valueOf(entry.getValue());
            }

            FontMetrics efm = g.getFontMetrics();
            int valW = efm.stringWidth(valueStr);
            g.drawString(valueStr, colValue - valW + 100, drawY);
        }
    }

    @Override
    public void keyPressed(int keyCode) {
        switch (keyCode) {
            case KeyEvent.VK_ESCAPE:
            case KeyEvent.VK_ENTER:
                manager.setState(GameStateManager.MENU);
                break;

            case KeyEvent.VK_LEFT:
                selectedCategory--;
                if (selectedCategory < 0) selectedCategory = CATEGORIES.length - 1;
                fetchLeaderboard();
                break;

            case KeyEvent.VK_RIGHT:
                selectedCategory++;
                if (selectedCategory >= CATEGORIES.length) selectedCategory = 0;
                fetchLeaderboard();
                break;

            case KeyEvent.VK_R:
                // Manual refresh
                fetchLeaderboard();
                break;
        }
    }

    @Override
    public void keyReleased(int keyCode) {
        // Not used
    }

    @Override
    public void cleanup() {
        // Nothing to clean up
    }

    /**
     * Format playtime into human-readable string (e.g. "1h 23m 45s")
     */
    private static String formatPlaytime(long totalSeconds) {
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }
}
