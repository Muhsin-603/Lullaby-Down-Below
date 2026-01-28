package com.buglife.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Utility class for exporting game state to JSON files for debugging.
 * Captures complete game state including player, entities, level config, and performance metrics.
 */
public class DebugExporter {
    private static final Logger logger = LoggerFactory.getLogger(DebugExporter.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    
    static {
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }
    
    /**
     * Export current game state to a JSON file.
     * 
     * @param levelName Current level name
     * @param playerX Player X position
     * @param playerY Player Y position
     * @param playerHunger Player hunger value
     * @param playerState Player state string
     * @param spiderCount Number of spiders
     * @param snailCount Number of snails
     * @param foodCount Number of food items
     */
    public static void exportGameState(
            String levelName,
            int playerX,
            int playerY,
            int playerHunger,
            String playerState,
            int spiderCount,
            int snailCount,
            int foodCount
    ) {
        try {
            ObjectNode root = mapper.createObjectNode();
            
            // Metadata
            ObjectNode metadata = mapper.createObjectNode();
            metadata.put("timestamp", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            metadata.put("levelName", levelName);
            metadata.put("exportVersion", "1.0");
            root.set("metadata", metadata);
            
            // Player state
            ObjectNode player = mapper.createObjectNode();
            player.put("x", playerX);
            player.put("y", playerY);
            player.put("hunger", playerHunger);
            player.put("state", playerState);
            root.set("player", player);
            
            // Entity counts
            ObjectNode entities = mapper.createObjectNode();
            entities.put("spiderCount", spiderCount);
            entities.put("snailCount", snailCount);
            entities.put("foodCount", foodCount);
            root.set("entities", entities);
            
            // Performance metrics
            PerformanceMonitor monitor = PerformanceMonitor.getInstance();
            ObjectNode performance = mapper.createObjectNode();
            performance.put("currentFPS", monitor.getCurrentFPS());
            performance.put("averageFPS", monitor.getAverageFPS());
            performance.put("totalFrames", monitor.getTotalFrames());
            performance.put("usedMemoryMB", monitor.getUsedMemoryMB());
            performance.put("maxMemoryMB", monitor.getMaxMemoryMB());
            performance.put("memoryUsagePercent", monitor.getMemoryUsagePercent());
            performance.put("updateTimeMs", monitor.getUpdateTimeMs());
            performance.put("renderTimeMs", monitor.getRenderTimeMs());
            root.set("performance", performance);
            
            // Debug settings
            ObjectNode debugSettings = mapper.createObjectNode();
            debugSettings.put("spiderPatrolEnabled", monitor.isSpiderPatrolEnabled());
            debugSettings.put("spiderDetectionEnabled", monitor.isSpiderDetectionEnabled());
            debugSettings.put("showHitboxes", monitor.isShowHitboxes());
            debugSettings.put("showTileGrid", monitor.isShowTileGrid());
            debugSettings.put("showSpiderPaths", monitor.isShowSpiderPaths());
            debugSettings.put("godMode", monitor.isGodModeEnabled());
            root.set("debugSettings", debugSettings);
            
            // Generate filename with timestamp
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String filename = String.format("debug_export_%s_%s.json", levelName, timestamp);
            
            // Create exports directory if it doesn't exist
            File exportsDir = new File("debug_exports");
            if (!exportsDir.exists()) {
                exportsDir.mkdirs();
            }
            
            // Write to file
            File outputFile = new File(exportsDir, filename);
            mapper.writeValue(outputFile, root);
            
            logger.info("Game state exported to: {}", outputFile.getAbsolutePath());
            
        } catch (Exception e) {
            logger.error("Failed to export game state", e);
        }
    }
    
    /**
     * Export with detailed entity information
     */
    public static void exportDetailedGameState(
            String levelName,
            int playerX,
            int playerY,
            int playerHunger,
            String playerState,
            double playerSpeed,
            boolean playerWebbed,
            boolean playerHasToy,
            java.util.List<int[]> spiderPositions,
            java.util.List<int[]> foodPositions
    ) {
        try {
            ObjectNode root = mapper.createObjectNode();
            
            // Metadata
            ObjectNode metadata = mapper.createObjectNode();
            metadata.put("timestamp", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            metadata.put("levelName", levelName);
            metadata.put("exportVersion", "1.1");
            metadata.put("detailedExport", true);
            root.set("metadata", metadata);
            
            // Player state
            ObjectNode player = mapper.createObjectNode();
            player.put("x", playerX);
            player.put("y", playerY);
            player.put("hunger", playerHunger);
            player.put("state", playerState);
            player.put("speed", playerSpeed);
            player.put("webbed", playerWebbed);
            player.put("hasToy", playerHasToy);
            root.set("player", player);
            
            // Spider positions
            ArrayNode spiders = mapper.createArrayNode();
            if (spiderPositions != null) {
                for (int[] pos : spiderPositions) {
                    ObjectNode spider = mapper.createObjectNode();
                    spider.put("x", pos[0]);
                    spider.put("y", pos[1]);
                    spiders.add(spider);
                }
            }
            root.set("spiders", spiders);
            
            // Food positions
            ArrayNode foods = mapper.createArrayNode();
            if (foodPositions != null) {
                for (int[] pos : foodPositions) {
                    ObjectNode food = mapper.createObjectNode();
                    food.put("x", pos[0]);
                    food.put("y", pos[1]);
                    foods.add(food);
                }
            }
            root.set("foods", foods);
            
            // Performance metrics
            PerformanceMonitor monitor = PerformanceMonitor.getInstance();
            ObjectNode performance = mapper.createObjectNode();
            performance.put("currentFPS", monitor.getCurrentFPS());
            performance.put("averageFPS", monitor.getAverageFPS());
            performance.put("totalFrames", monitor.getTotalFrames());
            performance.put("usedMemoryMB", monitor.getUsedMemoryMB());
            performance.put("maxMemoryMB", monitor.getMaxMemoryMB());
            root.set("performance", performance);
            
            // Generate filename
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String filename = String.format("debug_detailed_%s_%s.json", levelName, timestamp);
            
            // Create exports directory
            File exportsDir = new File("debug_exports");
            if (!exportsDir.exists()) {
                exportsDir.mkdirs();
            }
            
            // Write to file
            File outputFile = new File(exportsDir, filename);
            mapper.writeValue(outputFile, root);
            
            logger.info("Detailed game state exported to: {}", outputFile.getAbsolutePath());
            
        } catch (Exception e) {
            logger.error("Failed to export detailed game state", e);
        }
    }
}
