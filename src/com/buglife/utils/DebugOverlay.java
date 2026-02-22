package com.buglife.utils;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.awt.AlphaComposite;
import java.text.DecimalFormat;

/**
 * DebugOverlay - Renders comprehensive debug information overlay.
 * 
 * Resolves Issue #20: Expansion of the Debugging Suite
 * 
 * Displays:
 * - FPS (current + average) with color coding
 * - Memory usage with percentage bar
 * - Frame timing (update/render breakdown)
 * - Player coordinates and state
 * - Entity counts and states
 * - Performance graph
 * - Debug controls reference
 */
public class DebugOverlay {
    
    // UI Constants
    private static final Font TITLE_FONT = new Font("Consolas", Font.BOLD, 14);
    private static final Font MAIN_FONT = new Font("Consolas", Font.PLAIN, 12);
    private static final Font SMALL_FONT = new Font("Consolas", Font.PLAIN, 10);
    
    private static final Color BG_COLOR = new Color(0, 0, 0, 180);
    private static final Color BORDER_COLOR = new Color(100, 100, 100, 200);
    private static final Color TEXT_COLOR = new Color(255, 255, 255);
    private static final Color LABEL_COLOR = new Color(180, 180, 180);
    private static final Color GOOD_COLOR = new Color(100, 255, 100);
    private static final Color WARNING_COLOR = new Color(255, 200, 0);
    private static final Color CRITICAL_COLOR = new Color(255, 100, 100);
    
    private static final DecimalFormat FPS_FORMAT = new DecimalFormat("0.0");
    private static final DecimalFormat MEMORY_FORMAT = new DecimalFormat("0.0");
    private static final DecimalFormat TIME_FORMAT = new DecimalFormat("0.00");
    
    private static final int PADDING = 10;
    private static final int LINE_HEIGHT = 16;
    private static final int SECTION_SPACING = 8;
    
    /**
     * Render the complete debug overlay
     */
    public static void render(Graphics2D g, int screenWidth, int screenHeight) {
        PerformanceMonitor monitor = PerformanceMonitor.getInstance();
        
        if (!monitor.isDebugOverlayEnabled()) {
            return; // Don't render if overlay is off
        }
        
        // Enable anti-aliasing for smooth text
        g.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING,
                          java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        int x = PADDING;
        int y = PADDING;
        
        // Calculate overlay dimensions
        int overlayWidth = 400;
        int overlayHeight = 280;
        
        // Draw background panel
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
        g.setColor(BG_COLOR);
        g.fillRoundRect(x, y, overlayWidth, overlayHeight, 10, 10);
        
        // Draw border
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.8f));
        g.setColor(BORDER_COLOR);
        g.setStroke(new BasicStroke(2));
        g.drawRoundRect(x, y, overlayWidth, overlayHeight, 10, 10);
        
        // Reset composite for text
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        
        // Draw title
        g.setFont(TITLE_FONT);
        g.setColor(TEXT_COLOR);
        int textY = y + PADDING + 15;
        g.drawString("DEBUG OVERLAY (F3 to toggle)", x + PADDING, textY);
        
        textY += LINE_HEIGHT + SECTION_SPACING;
        
        // Draw sections
        textY = drawPerformanceSection(g, x + PADDING, textY, monitor);
        textY += SECTION_SPACING;
        
        textY = drawPlayerSection(g, x + PADDING, textY, monitor);
        textY += SECTION_SPACING;
        
        textY = drawEntitySection(g, x + PADDING, textY, monitor);
        textY += SECTION_SPACING;
        
        textY = drawControlsReference(g, x + PADDING, textY);
    }
    
    /**
     * Draw performance metrics section
     */
    private static int drawPerformanceSection(Graphics2D g, int x, int y, PerformanceMonitor monitor) {
        g.setFont(MAIN_FONT);
        
        // FPS display with color coding
        double fps = monitor.getCurrentFPS();
        double avgFps = monitor.getAverageFPS();
        
        Color fpsColor = getFPSColor(fps);
        g.setColor(LABEL_COLOR);
        g.drawString("FPS:", x, y);
        g.setColor(fpsColor);
        String fpsText = FPS_FORMAT.format(fps) + " (avg: " + FPS_FORMAT.format(avgFps) + ")";
        g.drawString(fpsText, x + 40, y);
        
        y += LINE_HEIGHT;
        
        // Memory usage with bar
        double usedMB = monitor.getUsedMemoryMB();
        double maxMB = monitor.getMaxMemoryMB();
        double memPercent = monitor.getMemoryUsagePercent();
        
        g.setColor(LABEL_COLOR);
        g.drawString("Memory:", x, y);
        g.setColor(TEXT_COLOR);
        String memText = MEMORY_FORMAT.format(usedMB) + " / " + MEMORY_FORMAT.format(maxMB) + " MB";
        g.drawString(memText, x + 70, y);
        
        // Memory percentage bar
        int barX = x + 220;
        int barY = y - 10;
        int barWidth = 100;
        int barHeight = 8;
        
        // Background
        g.setColor(new Color(50, 50, 50));
        g.fillRect(barX, barY, barWidth, barHeight);
        
        // Fill
        int fillWidth = (int)(barWidth * (memPercent / 100.0));
        Color memColor = getMemoryColor(memPercent);
        g.setColor(memColor);
        g.fillRect(barX, barY, fillWidth, barHeight);
        
        // Border
        g.setColor(BORDER_COLOR);
        g.drawRect(barX, barY, barWidth, barHeight);
        
        // Percentage text
        g.setFont(SMALL_FONT);
        g.setColor(TEXT_COLOR);
        g.drawString((int)memPercent + "%", barX + barWidth + 5, y);
        
        y += LINE_HEIGHT;
        g.setFont(MAIN_FONT);
        
        // Frame timing
        double updateMs = monitor.getUpdateTimeMs();
        double renderMs = monitor.getRenderTimeMs();
        double totalMs = updateMs + renderMs;
        
        g.setColor(LABEL_COLOR);
        g.drawString("Timing:", x, y);
        g.setColor(TEXT_COLOR);
        String timingText = "Update: " + TIME_FORMAT.format(updateMs) + "ms  " +
                           "Render: " + TIME_FORMAT.format(renderMs) + "ms  " +
                           "Total: " + TIME_FORMAT.format(totalMs) + "ms";
        g.drawString(timingText, x + 70, y);
        
        y += LINE_HEIGHT;
        
        // Coordinates and level
        int playerX = monitor.getPlayerX();
        int playerY = monitor.getPlayerY();
        String level = monitor.getCurrentLevel();
        
        g.setColor(LABEL_COLOR);
        g.drawString("Position:", x, y);
        g.setColor(TEXT_COLOR);
        g.drawString("(" + playerX + ", " + playerY + ")", x + 70, y);
        
        g.setColor(LABEL_COLOR);
        g.drawString("Level:", x + 200, y);
        g.setColor(TEXT_COLOR);
        g.drawString(level, x + 250, y);
        
        y += LINE_HEIGHT;
        
        return y;
    }
    
    /**
     * Draw player state section
     */
    private static int drawPlayerSection(Graphics2D g, int x, int y, PerformanceMonitor monitor) {
        g.setFont(MAIN_FONT);
        g.setColor(new Color(100, 200, 255)); // Section header color
        g.drawString("PLAYER:", x, y);
        
        y += LINE_HEIGHT;
        
        // State
        g.setColor(LABEL_COLOR);
        g.drawString("  State:", x, y);
        g.setColor(TEXT_COLOR);
        g.drawString(monitor.getPlayerState(), x + 80, y);
        
        // Hunger
        int hunger = monitor.getPlayerHunger();
        Color hungerColor = getHungerColor(hunger);
        g.setColor(LABEL_COLOR);
        g.drawString("Hunger:", x + 180, y);
        g.setColor(hungerColor);
        g.drawString(hunger + "/100", x + 250, y);
        
        y += LINE_HEIGHT;
        
        // Speed
        g.setColor(LABEL_COLOR);
        g.drawString("  Speed:", x, y);
        g.setColor(TEXT_COLOR);
        g.drawString(String.format("%.1f", monitor.getPlayerSpeed()), x + 80, y);
        
        // Webbed status
        g.setColor(LABEL_COLOR);
        g.drawString("Webbed:", x + 180, y);
        boolean webbed = monitor.isPlayerWebbed();
        g.setColor(webbed ? CRITICAL_COLOR : GOOD_COLOR);
        g.drawString(webbed ? "YES" : "No", x + 250, y);
        
        y += LINE_HEIGHT;
        
        // Has toy
        g.setColor(LABEL_COLOR);
        g.drawString("  Has Toy:", x, y);
        boolean hasToy = monitor.isPlayerHasToy();
        g.setColor(hasToy ? GOOD_COLOR : LABEL_COLOR);
        g.drawString(hasToy ? "Yes" : "No", x + 80, y);
        
        y += LINE_HEIGHT;
        
        return y;
    }
    
    /**
     * Draw entity counts section
     */
    private static int drawEntitySection(Graphics2D g, int x, int y, PerformanceMonitor monitor) {
        g.setFont(MAIN_FONT);
        g.setColor(new Color(255, 150, 100)); // Section header color
        g.drawString("ENTITIES:", x, y);
        
        y += LINE_HEIGHT;
        
        g.setColor(LABEL_COLOR);
        g.drawString("  Spiders:", x, y);
        g.setColor(TEXT_COLOR);
        g.drawString(String.valueOf(monitor.getSpiderCount()), x + 80, y);
        
        g.setColor(LABEL_COLOR);
        g.drawString("Snails:", x + 180, y);
        g.setColor(TEXT_COLOR);
        g.drawString(String.valueOf(monitor.getSnailCount()), x + 250, y);
        
        y += LINE_HEIGHT;
        
        g.setColor(LABEL_COLOR);
        g.drawString("  Foods:", x, y);
        g.setColor(TEXT_COLOR);
        g.drawString(String.valueOf(monitor.getFoodCount()), x + 80, y);
        
        y += LINE_HEIGHT;
        
        return y;
    }
    
    /**
     * Draw controls reference
     */
    private static int drawControlsReference(Graphics2D g, int x, int y) {
        g.setFont(SMALL_FONT);
        g.setColor(new Color(150, 150, 150));
        g.drawString("[F3] Overlay  [F4] Hitboxes  [F6] Paths  [F7] God Mode", x, y);
        
        y += LINE_HEIGHT - 2;
        
        return y;
    }
    
    /**
     * Get color based on FPS value
     */
    private static Color getFPSColor(double fps) {
        if (fps >= 55) {
            return GOOD_COLOR;
        } else if (fps >= 30) {
            return WARNING_COLOR;
        } else {
            return CRITICAL_COLOR;
        }
    }
    
    /**
     * Get color based on memory usage percentage
     */
    private static Color getMemoryColor(double percent) {
        if (percent < 70) {
            return GOOD_COLOR;
        } else if (percent < 85) {
            return WARNING_COLOR;
        } else {
            return CRITICAL_COLOR;
        }
    }
    
    /**
     * Get color based on hunger value
     */
    private static Color getHungerColor(int hunger) {
        if (hunger >= 60) {
            return GOOD_COLOR;
        } else if (hunger >= 30) {
            return WARNING_COLOR;
        } else {
            return CRITICAL_COLOR;
        }
    }
}