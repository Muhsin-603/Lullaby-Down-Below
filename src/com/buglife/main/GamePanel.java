package com.buglife.main;

import com.buglife.assets.SoundManager;
import com.buglife.utils.PerformanceMonitor;
import javax.swing.JPanel;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.Color;
 

public class GamePanel extends JPanel {
    public static final int VIRTUAL_WIDTH = 1366;
    public static final int VIRTUAL_HEIGHT = 768;

    private GameStateManager stateManager;
    private SoundManager soundManager;

    public GamePanel(SoundManager sm) {
        this.soundManager = sm;
        this.stateManager = new GameStateManager(soundManager, this);

        setBackground(Color.BLACK);
        setPreferredSize(new Dimension(VIRTUAL_WIDTH, VIRTUAL_HEIGHT));
        setFocusable(true);
        addKeyListener(new KeyInputAdapter());
    }

    public void updateGame() {
        stateManager.update();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();

        // --- SCALING CALCULATIONS ---
        int realScreenWidth = getWidth();
        int realScreenHeight = getHeight();

        double scaleX = (double) realScreenWidth / VIRTUAL_WIDTH;
        double scaleY = (double) realScreenHeight / VIRTUAL_HEIGHT;
        double scale = Math.min(scaleX, scaleY);

        int scaledWidth = (int) (VIRTUAL_WIDTH * scale);
        int scaledHeight = (int) (VIRTUAL_HEIGHT * scale);

        int xOffset = (realScreenWidth - scaledWidth) / 2;
        int yOffset = (realScreenHeight - scaledHeight) / 2;

        // --- APPLY TRANSFORMATION ---
        g2d.translate(xOffset, yOffset);
        g2d.scale(scale, scale);

        // --- DELEGATE TO STATE MANAGER ---
        stateManager.draw(g2d);

        // --- DRAW PERFORMANCE OVERLAY ---
        if (PerformanceMonitor.getInstance().isDebugOverlayEnabled()) {
            drawPerformanceOverlay(g2d);
        }

        g2d.dispose();
    }

    private void drawPerformanceOverlay(Graphics2D g) {
        PerformanceMonitor monitor = PerformanceMonitor.getInstance();
        
        // Calculate overlay height dynamically
        int baseHeight = 200;  // Base height for main info
        int spiderMenuHeight = monitor.isSpiderTogglesVisible() ? 100 : 0;
        int totalHeight = baseHeight + spiderMenuHeight;
        
        // Semi-transparent black background
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRect(10, 10, 450, totalHeight);
        
        // White text for stats
        g.setColor(Color.WHITE);
        g.setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 14));
        
        int y = 30;
        
        // Performance metrics
        g.drawString(String.format("FPS: %.1f (avg: %.1f)", 
                monitor.getCurrentFPS(), monitor.getAverageFPS()), 20, y);
        y += 20;
        g.drawString(String.format("Frames: %d | Update: %.2fms | Render: %.2fms", 
                monitor.getTotalFrames(), monitor.getUpdateTimeMs(), monitor.getRenderTimeMs()), 20, y);
        y += 20;
        g.drawString(String.format("Memory: %.1f/%.1f MB (%.1f%%)", 
                monitor.getUsedMemoryMB(), monitor.getMaxMemoryMB(), 
                monitor.getMemoryUsagePercent()), 20, y);
        y += 20;
        
        // Player info
        g.drawString(String.format("Player: X:%d Y:%d | State: %s", 
                monitor.getPlayerX(), monitor.getPlayerY(), monitor.getPlayerState()), 20, y);
        y += 20;
        g.drawString(String.format("Hunger: %d | Speed: %.1f | Webbed: %s | Toy: %s", 
                monitor.getPlayerHunger(), monitor.getPlayerSpeed(), 
                monitor.isPlayerWebbed() ? "YES" : "NO",
                monitor.isPlayerHasToy() ? "YES" : "NO"), 20, y);
        y += 20;
        
        // Entity counts
        g.drawString(String.format("Level: %s | Entities - Spiders: %d, Snails: %d, Food: %d", 
                monitor.getCurrentLevel(), monitor.getSpiderCount(), 
                monitor.getSnailCount(), monitor.getFoodCount()), 20, y);
        y += 20;
        
        // Controls
        g.setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 12));
        g.drawString("F3:Debug F2:Spider F4:Hitbox F5:Grid F6:Paths F7:God F12:Export", 20, y);
        y += 25;
        
        // Frame time graph (mini bars)
        g.setColor(Color.GREEN);
        double[] frameTimes = monitor.getFrameTimes();
        int graphX = 20;
        int graphY = y;
        int barWidth = 2;
        int maxBarHeight = 30;
        
        for (int i = 0; i < frameTimes.length; i++) {
            double frameTime = frameTimes[i];
            int barHeight = (int) Math.min(frameTime, maxBarHeight);
            
            // Color code: green < 16ms, yellow < 20ms, red >= 20ms
            if (frameTime < 16) {
                g.setColor(Color.GREEN);
            } else if (frameTime < 20) {
                g.setColor(Color.YELLOW);
            } else {
                g.setColor(Color.RED);
            }
            
            g.fillRect(graphX + i * (barWidth + 1), graphY + maxBarHeight - barHeight, barWidth, barHeight);
        }
        
        y += maxBarHeight + 10;
        g.setColor(Color.WHITE);
        g.setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 10));
        g.drawString("Frame Time (60 frames, 16ms target)", 20, y);
        
        // Spider debug menu (if visible)
        if (monitor.isSpiderTogglesVisible()) {
            y += 20;
            g.setFont(new java.awt.Font("Monospaced", java.awt.Font.BOLD, 14));
            g.setColor(Color.CYAN);
            g.drawString("=== SPIDER DEBUG (F2) ===", 20, y);
            
            y += 20;
            g.setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 14));
            g.setColor(Color.WHITE);
            
            String patrolStatus = monitor.isSpiderPatrolEnabled() ? "[X] Patrol Enabled" : "[ ] Patrol Disabled";
            g.drawString(patrolStatus + " (Press P)", 20, y);
            
            y += 20;
            String detectionStatus = monitor.isSpiderDetectionEnabled() ? "[X] Detection Enabled" : "[ ] Detection Disabled";
            g.drawString(detectionStatus + " (Press Y)", 20, y);
            
            y += 20;
            g.setColor(Color.GRAY);
            g.setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 12));
            g.drawString("Red outline = patrol frozen | Blue outline = detection off", 20, y);
        }
    }

    private class KeyInputAdapter extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            PerformanceMonitor monitor = PerformanceMonitor.getInstance();
            
            // F3 key toggles performance overlay
            if (e.getKeyCode() == KeyEvent.VK_F3) {
                monitor.toggleDebugOverlay();
                return;
            }
            
            // F2 key toggles spider debug menu
            if (e.getKeyCode() == KeyEvent.VK_F2) {
                monitor.toggleSpiderTogglesMenu();
                return;
            }
            
            // F4 key toggles hitboxes
            if (e.getKeyCode() == KeyEvent.VK_F4) {
                monitor.toggleHitboxes();
                return;
            }
            
            // F5 key toggles tile grid
            if (e.getKeyCode() == KeyEvent.VK_F5) {
                monitor.toggleTileGrid();
                return;
            }
            
            // F6 key toggles spider paths
            if (e.getKeyCode() == KeyEvent.VK_F6) {
                monitor.toggleSpiderPaths();
                return;
            }
            
            // F7 key toggles god mode
            if (e.getKeyCode() == KeyEvent.VK_F7) {
                monitor.toggleGodMode();
                return;
            }
            
            // P key toggles spider patrol (when spider menu is visible)
            if (e.getKeyCode() == KeyEvent.VK_P && monitor.isSpiderTogglesVisible()) {
                monitor.toggleSpiderPatrol();
                return;
            }
            
            // D key toggles spider detection (when spider menu is visible)
            if (e.getKeyCode() == KeyEvent.VK_Y && monitor.isSpiderTogglesVisible()) {
                monitor.toggleSpiderDetection();
                return;
            }
            
            stateManager.keyPressed(e.getKeyCode());
        }

        @Override
        public void keyReleased(KeyEvent e) {
            stateManager.keyReleased(e.getKeyCode());
        }
    }
}