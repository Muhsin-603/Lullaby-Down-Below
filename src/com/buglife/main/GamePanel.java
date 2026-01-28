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
        
        // Semi-transparent black background
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRect(10, 10, 350, 100);
        
        // White text for stats
        g.setColor(Color.WHITE);
        g.setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 14));
        
        int y = 30;
        g.drawString(String.format("FPS: %.1f (avg: %.1f)", 
                monitor.getCurrentFPS(), monitor.getAverageFPS()), 20, y);
        y += 20;
        g.drawString(String.format("Frames: %d", monitor.getTotalFrames()), 20, y);
        y += 20;
        g.drawString(String.format("Memory: %.1f/%.1f MB (%.1f%%)", 
                monitor.getUsedMemoryMB(), monitor.getMaxMemoryMB(), 
                monitor.getMemoryUsagePercent()), 20, y);
        y += 20;
        g.drawString("Press F3 to toggle", 20, y);
    }

    private class KeyInputAdapter extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            // F3 key toggles performance overlay
            if (e.getKeyCode() == KeyEvent.VK_F3) {
                PerformanceMonitor.getInstance().toggleDebugOverlay();
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