package com.buglife.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Performance monitoring utility for tracking FPS, memory usage, and game metrics.
 * Provides debug information overlay and performance logging.
 */
public class PerformanceMonitor {
    private static final Logger logger = LoggerFactory.getLogger(PerformanceMonitor.class);
    private static PerformanceMonitor instance;
    
    // private long lastFrameTime;
    private long frameCount;
    private double currentFPS;
    private double averageFPS;
    private long fpsUpdateTime;
    
    private long totalFrames;
    private long startTime;
    
    // Memory tracking
    private long usedMemory;
    private long maxMemory;
    private long totalMemory;
    
    // Performance thresholds
    private static final double TARGET_FPS = 60.0;
    private static final long FPS_UPDATE_INTERVAL = 1000; // milliseconds
    private static final long MEMORY_CHECK_INTERVAL = 5000; // milliseconds
    
    private long lastMemoryCheck;
    private boolean showDebugOverlay;
    
    private PerformanceMonitor() {
        this.startTime = System.nanoTime();
        // this.lastFrameTime = System.nanoTime();
        this.fpsUpdateTime = System.currentTimeMillis();
        this.lastMemoryCheck = System.currentTimeMillis();
        this.showDebugOverlay = false;
    }
    
    public static PerformanceMonitor getInstance() {
        if (instance == null) {
            instance = new PerformanceMonitor();
        }
        return instance;
    }
    
    /**
     * Update performance metrics. Call once per frame.
     */
    public void update() {
        long currentTime = System.nanoTime();
        long currentMillis = System.currentTimeMillis();
        
        // Update frame timing
        frameCount++;
        totalFrames++;
        
        // Calculate FPS every second
        if (currentMillis - fpsUpdateTime >= FPS_UPDATE_INTERVAL) {
            double elapsedSeconds = (currentMillis - fpsUpdateTime) / 1000.0;
            currentFPS = frameCount / elapsedSeconds;
            averageFPS = totalFrames / ((currentTime - startTime) / 1_000_000_000.0);
            
            frameCount = 0;
            fpsUpdateTime = currentMillis;
            
            // Log warning if FPS drops below target
            if (currentFPS < TARGET_FPS * 0.9) {
                logger.warn("FPS drop detected: {:.1f} FPS (target: {})", currentFPS, TARGET_FPS);
            }
        }
        
        // Update memory stats periodically
        if (currentMillis - lastMemoryCheck >= MEMORY_CHECK_INTERVAL) {
            updateMemoryStats();
            lastMemoryCheck = currentMillis;
            
            // Log memory usage
            logger.debug("Memory: {} MB / {} MB (max: {} MB)", 
                    usedMemory / 1_048_576, 
                    totalMemory / 1_048_576, 
                    maxMemory / 1_048_576);
        }
        
        // lastFrameTime = currentTime;
    }
    
    /**
     * Update memory statistics
     */
    private void updateMemoryStats() {
        Runtime runtime = Runtime.getRuntime();
        totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        maxMemory = runtime.maxMemory();
        usedMemory = totalMemory - freeMemory;
        
        // Warn if memory usage is high
        double memoryUsagePercent = (double) usedMemory / maxMemory * 100;
        if (memoryUsagePercent > 80) {
            logger.warn("High memory usage: {:.1f}%", memoryUsagePercent);
        }
    }
    
    /**
     * Get current FPS
     */
    public double getCurrentFPS() {
        return currentFPS;
    }
    
    /**
     * Get average FPS since start
     */
    public double getAverageFPS() {
        return averageFPS;
    }
    
    /**
     * Get total frames rendered
     */
    public long getTotalFrames() {
        return totalFrames;
    }
    
    /**
     * Get used memory in bytes
     */
    public long getUsedMemory() {
        return usedMemory;
    }
    
    /**
     * Get used memory in megabytes
     */
    public double getUsedMemoryMB() {
        return usedMemory / 1_048_576.0;
    }
    
    /**
     * Get total available memory in megabytes
     */
    public double getTotalMemoryMB() {
        return totalMemory / 1_048_576.0;
    }
    
    /**
     * Get maximum memory in megabytes
     */
    public double getMaxMemoryMB() {
        return maxMemory / 1_048_576.0;
    }
    
    /**
     * Get memory usage percentage
     */
    public double getMemoryUsagePercent() {
        return (double) usedMemory / maxMemory * 100.0;
    }
    
    /**
     * Toggle debug overlay
     */
    public void toggleDebugOverlay() {
        showDebugOverlay = !showDebugOverlay;
        logger.info("Debug overlay: {}", showDebugOverlay ? "ON" : "OFF");
    }
    
    /**
     * Check if debug overlay should be shown
     */
    public boolean isDebugOverlayEnabled() {
        return showDebugOverlay;
    }
    
    /**
     * Set debug overlay state
     */
    public void setDebugOverlay(boolean enabled) {
        this.showDebugOverlay = enabled;
    }
    
    /**
     * Get formatted performance statistics string
     */
    public String getStats() {
        return String.format(
            "FPS: %.1f (avg: %.1f) | Frames: %d | Memory: %.1f/%.1f MB (%.1f%%)",
            currentFPS, averageFPS, totalFrames,
            getUsedMemoryMB(), getMaxMemoryMB(), getMemoryUsagePercent()
        );
    }
    
    /**
     * Force garbage collection (use sparingly)
     */
    public void forceGC() {
        logger.info("Forcing garbage collection...");
        long beforeGC = usedMemory;
        System.gc();
        updateMemoryStats();
        long freedMemory = beforeGC - usedMemory;
        logger.info("GC complete. Freed {} MB", freedMemory / 1_048_576);
    }
    
    /**
     * Reset statistics
     */
    public void reset() {
        frameCount = 0;
        totalFrames = 0;
        currentFPS = 0;
        averageFPS = 0;
        startTime = System.nanoTime();
        fpsUpdateTime = System.currentTimeMillis();
        logger.info("Performance monitor reset");
    }
    
    /**
     * Log current performance summary
     */
    public void logSummary() {
        logger.info("=== Performance Summary ===");
        logger.info("Current FPS: {:.1f}", currentFPS);
        logger.info("Average FPS: {:.1f}", averageFPS);
        logger.info("Total Frames: {}", totalFrames);
        logger.info("Memory Usage: {:.1f} MB / {:.1f} MB ({:.1f}%)", 
                getUsedMemoryMB(), getMaxMemoryMB(), getMemoryUsagePercent());
        logger.info("========================");
    }
}
