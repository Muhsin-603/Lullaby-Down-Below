package com.buglife.engine.editor.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Handles loading and saving level data in JSON format.
 * Also supports importing/exporting legacy text map format
 * and generating Java config snippets.
 */
public class LevelDataIO {
    
    private static final ObjectMapper mapper = new ObjectMapper()
        .enable(SerializationFeature.INDENT_OUTPUT);
    


    public static void saveToJson(LevelData data, File file) throws IOException {
        // Create backup if file exists
        if (file.exists()) {
            createBackup(file);
        }
        mapper.writeValue(file, data);
        
    }
    
    /**
     * Save level data to JSON file (Path version).
     */
    public static void saveToJson(LevelData data, Path path) throws IOException {
        saveToJson(data, path.toFile());
    }

    /**
     * Load level data from JSON file.
     */
    public static LevelData loadFromJson(File file) throws IOException {
        return mapper.readValue(file, LevelData.class);
    }

    /**
     * Load level data from JSON file (Path version).
     */
    public static LevelData loadFromJson(Path path) throws IOException {
        return loadFromJson(path.toFile());
    }
    
    
    // ========== LEGACY TEXT MAP IMPORT/EXPORT ==========
    
    /**
     * Import tile data from legacy text map format.
     * Only imports tiles - entities come from LevelConfig.
     */
    public static LevelData importFromTextMap(File mapFile) throws IOException {
        return importFromTextMap(mapFile, mapFile.getName().replace(".txt", ""));
    }

    /**
     * Import tile data from legacy text map format (Path version).
     */
    public static LevelData importFromTextMap(Path mapPath, String name) throws IOException {
        return importFromTextMap(mapPath.toFile(), name);
    }

    /**
     * Import tile data from legacy text map format with custom name.
     */
    public static LevelData importFromTextMap(File mapFile, String name) throws IOException {
        List<List<Integer>> rows = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(mapFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                List<Integer> row = new ArrayList<>();
                String[] nums = line.trim().split("\\s+");
                for (String num : nums) {
                    row.add(Integer.parseInt(num));
                }
                rows.add(row);
            }
        }
        
        if (rows.isEmpty()) {
            throw new IOException("Empty map file");
        }
        
        int height = rows.size();
        int width = rows.get(0).size();
        
        LevelData data = new LevelData(name, width, height);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                data.setTile(x, y, rows.get(y).get(x));
            }
        }
        
        return data;
    }

    private static void createBackup(File file) {
        try {
            Path backupDir = file.toPath().getParent().resolve("backups");
            if (!Files.exists(backupDir)) {
                Files.createDirectories(backupDir);
            }
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String backupName = file.getName() + "." + timestamp + ".bak";
            Files.copy(file.toPath(), backupDir.resolve(backupName), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.err.println("Failed to create backup: " + e.getMessage());
        }
    }
    
    /**
     * Export tile data to legacy text map format.
     */
    public static void exportToTextMap(LevelData data, File file) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            for (int y = 0; y < data.getHeight(); y++) {
                StringBuilder line = new StringBuilder();
                for (int x = 0; x < data.getWidth(); x++) {
                    if (x > 0) line.append(" ");
                    line.append(data.getTile(x, y));
                }
                writer.println(line);
            }
        }
    }

    /**
     * Export tile data to legacy text map format (Path version).
     */
    public static void exportToTextMap(LevelData data, Path path) throws IOException {
        exportToTextMap(data, path.toFile());
    }
    
    // ========== JAVA CONFIG GENERATION ==========
    
    /**
     * Generate Java config class code from level data.
     * This creates a LevelXConfig.java snippet that can be pasted into the codebase.
     */
    public static String generateJavaConfig(LevelData data) {
        return generateJavaConfig(data, toPascalCase(data.getLevelName()) + "Config");
    }

    /**
     * Generate Java config class code from level data with custom class name.
     */
    public static String generateJavaConfig(LevelData data, String className) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("package com.buglife.levels;\n\n");
        sb.append("import java.awt.Point;\n");
        sb.append("import java.util.Arrays;\n");
        sb.append("import java.util.List;\n\n");
        sb.append("/**\n");
        sb.append(" * Auto-generated level configuration.\n");
        sb.append(" * Level: ").append(data.getLevelName()).append("\n");
        if (!data.getDescription().isEmpty()) {
            sb.append(" * Description: ").append(data.getDescription()).append("\n");
        }
        sb.append(" */\n");
        sb.append("public class ").append(className).append(" implements LevelConfig {\n\n");
        
        // getLevelName
        sb.append("    @Override\n");
        sb.append("    public String getLevelName() {\n");
        sb.append("        return \"").append(data.getLevelName()).append("\";\n");
        sb.append("    }\n\n");
        
        // getPlayerSpawn
        sb.append("    @Override\n");
        sb.append("    public Point getPlayerSpawn() {\n");
        if (data.getPlayerSpawn() != null) {
            sb.append("        return new Point(").append(data.getPlayerSpawn().x)
              .append(", ").append(data.getPlayerSpawn().y).append(");\n");
        } else {
            sb.append("        return new Point(128, 128);\n");
        }
        sb.append("    }\n\n");
        
        // getMechanicsEnabled
        sb.append("    @Override\n");
        sb.append("    public MechanicsConfig getMechanicsEnabled() {\n");
        sb.append("        return new MechanicsConfig()");
        LevelData.MechanicsData m = data.getMechanics();
        if (m.dashEnabled) sb.append("\n            .enableDash()");
        if (m.toyEnabled) sb.append("\n            .enableToy()");
        if (m.tripWiresEnabled) sb.append("\n            .enableTripWires()");
        if (m.speedBoostFoodEnabled) sb.append("\n            .enableSpeedBoostFood()");
        sb.append(";\n");
        sb.append("    }\n\n");
        
        // getToySpawn
        sb.append("    @Override\n");
        sb.append("    public Point getToySpawn() {\n");
        if (data.getToySpawn() != null) {
            sb.append("        return new Point(").append(data.getToySpawn().x)
              .append(", ").append(data.getToySpawn().y).append(");\n");
        } else {
            sb.append("        return null;\n");
        }
        sb.append("    }\n\n");
        
        // getTripWirePositions
        sb.append("    @Override\n");
        sb.append("    public List<Point> getTripWirePositions() {\n");
        if (data.getTripwires().isEmpty()) {
            sb.append("        return Arrays.asList();\n");
        } else {
            sb.append("        return Arrays.asList(\n");
            for (int i = 0; i < data.getTripwires().size(); i++) {
                LevelData.PointData p = data.getTripwires().get(i);
                sb.append("            new Point(").append(p.x).append(", ").append(p.y).append(")");
                if (i < data.getTripwires().size() - 1) sb.append(",");
                sb.append("\n");
            }
            sb.append("        );\n");
        }
        sb.append("    }\n\n");
        
        // getSpiderPatrols
        sb.append("    @Override\n");
        sb.append("    public List<SpiderPatrolData> getSpiderPatrols() {\n");
        if (data.getSpiders().isEmpty()) {
            sb.append("        return Arrays.asList();\n");
        } else {
            sb.append("        return Arrays.asList(\n");
            for (int i = 0; i < data.getSpiders().size(); i++) {
                LevelData.SpiderData spider = data.getSpiders().get(i);
                sb.append("            SpiderPatrolData.custom()");
                for (LevelData.PointData wp : spider.waypoints) {
                    sb.append("\n                .addPoint(").append(wp.x).append(", ").append(wp.y).append(")");
                }
                sb.append("\n                .build()");
                if (!spider.description.isEmpty()) {
                    sb.append(".describe(\"").append(spider.description).append("\")");
                }
                if (i < data.getSpiders().size() - 1) sb.append(",");
                sb.append("\n");
            }
            sb.append("        );\n");
        }
        sb.append("    }\n\n");
        
        // getSnailLocations
        sb.append("    @Override\n");
        sb.append("    public List<SnailLocationData> getSnailLocations() {\n");
        if (data.getSnails().isEmpty()) {
            sb.append("        return Arrays.asList();\n");
        } else {
            sb.append("        return Arrays.asList(\n");
            for (int i = 0; i < data.getSnails().size(); i++) {
                LevelData.SnailData snail = data.getSnails().get(i);
                sb.append("            SnailLocationData.at(")
                  .append(snail.position.x).append(", ").append(snail.position.y).append(")\n");
                if (!snail.dialogue.isEmpty()) {
                    sb.append("                .withDialogue(");
                    for (int j = 0; j < snail.dialogue.size(); j++) {
                        if (j > 0) sb.append(", ");
                        sb.append("\"").append(escapeJava(snail.dialogue.get(j))).append("\"");
                    }
                    sb.append(")\n");
                }
                sb.append("                .").append(snail.requiresInteraction ? "requiresInteraction()" : "autoAdvance()");
                if (i < data.getSnails().size() - 1) sb.append(",");
                sb.append("\n");
            }
            sb.append("        );\n");
        }
        sb.append("    }\n\n");
        
        // getFoodSpawns
        sb.append("    @Override\n");
        sb.append("    public List<FoodSpawnData> getFoodSpawns() {\n");
        if (data.getFood().isEmpty()) {
            sb.append("        return Arrays.asList();\n");
        } else {
            sb.append("        return Arrays.asList(\n");
            for (int i = 0; i < data.getFood().size(); i++) {
                LevelData.FoodData food = data.getFood().get(i);
                String method = food.type.equals("ENERGY_SEED") ? "energySeed" : "berry";
                sb.append("            FoodSpawnData.").append(method).append("(")
                  .append(food.position.x).append(", ").append(food.position.y).append(")");
                if (i < data.getFood().size() - 1) sb.append(",");
                sb.append("\n");
            }
            sb.append("        );\n");
        }
        sb.append("    }\n");
        
        sb.append("}\n");
        
        return sb.toString();
    }
    
    private static String toPascalCase(String input) {
        StringBuilder sb = new StringBuilder();
        boolean capitalizeNext = true;
        for (char c : input.toCharArray()) {
            if (c == '_' || c == '-' || c == ' ') {
                capitalizeNext = true;
            } else if (capitalizeNext) {
                sb.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
    
    private static String escapeJava(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
