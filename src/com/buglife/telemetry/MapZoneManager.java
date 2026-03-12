package com.buglife.telemetry;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.buglife.config.GameConstants;

/**
 * MapZoneManager — Divides each level into a grid of rectangular zones
 * and assigns each zone an area_code for telemetry tracking.
 *
 * Each level's map (in pixels) is split into a grid of cells.
 * The cell size defaults to {@value #ZONE_SIZE} pixels (8 tiles × 64px).
 * Each cell gets a unique area_code derived from a combination of level
 * index and grid position.
 *
 * The mapzone table in the database stores zone metadata:
 *   area_code  — unique identifier (level_index * 10000 + row * 100 + col)
 *   level_name — e.g. "level1"
 *   zone_name  — human-readable label e.g. "level1_R0_C0"
 *   x_min, y_min, x_max, y_max — bounding box in world pixels
 *
 * Usage from TelemetryClient:
 *   MapZoneManager.ensureZonesPopulated(db)  — idempotent DB insert
 *   MapZoneManager.computeAreaCode(level, x, y) — pure math, no DB hit
 */
public final class MapZoneManager {
    private static final Logger logger = LoggerFactory.getLogger(MapZoneManager.class);

    /** Zone cell size in pixels (8 tiles wide/tall at 64 px per tile) */
    private static final int ZONE_SIZE = 512;

    /** Level name → level index for area_code encoding */
    private static final Map<String, Integer> LEVEL_INDEX = new HashMap<>();

    /** Level name → { rows, cols } in tiles */
    private static final Map<String, int[]> LEVEL_DIMENSIONS = new HashMap<>();

    // SQL for mapzone table
    private static final String SQL_ZONE_EXISTS =
            "SELECT 1 FROM mapzone WHERE area_code = ? LIMIT 1";
    private static final String SQL_INSERT_ZONE =
            "INSERT INTO mapzone (area_code, level_name, zone_name, x_min, y_min, x_max, y_max) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?)";

    private static volatile boolean populated = false;

    static {
        // known levels — index is used in area_code formula
        LEVEL_INDEX.put("level_test", 0);
        LEVEL_INDEX.put("level1", 1);
        LEVEL_INDEX.put("level2", 2);
        LEVEL_INDEX.put("level3", 3);
        LEVEL_INDEX.put("level4", 4);
        LEVEL_INDEX.put("level5", 5);

        // dimensions in tiles  { rows, cols }
        LEVEL_DIMENSIONS.put("level_test", new int[]{78, 36});
        LEVEL_DIMENSIONS.put("level1",     new int[]{75, 36});
        LEVEL_DIMENSIONS.put("level2",     new int[]{103, 55});
        LEVEL_DIMENSIONS.put("level3",     new int[]{89, 55});
        LEVEL_DIMENSIONS.put("level4",     new int[]{89, 55});
        LEVEL_DIMENSIONS.put("level5",     new int[]{89, 55});
    }

    // Prevent instantiation
    private MapZoneManager() {
        throw new AssertionError("Utility class");
    }

    // ====================================================================
    // PUBLIC API
    // ====================================================================

    /**
     * Compute the area_code for a world-pixel coordinate on a given level.
     * Pure math — no database access required.
     *
     * Formula: levelIndex * 10000 + gridRow * 100 + gridCol
     *
     * @param levelName e.g. "level1"
     * @param worldX    world-pixel X coordinate
     * @param worldY    world-pixel Y coordinate
     * @return area_code ≥ 0, or -1 if the level is unknown
     */
    public static int computeAreaCode(String levelName, double worldX, double worldY) {
        Integer levelIdx = LEVEL_INDEX.get(levelName);
        if (levelIdx == null) {
            return -1;
        }

        // Clamp negatives to 0
        int px = Math.max(0, (int) worldX);
        int py = Math.max(0, (int) worldY);

        int gridCol = px / ZONE_SIZE;
        int gridRow = py / ZONE_SIZE;

        return levelIdx * 10_000 + gridRow * 100 + gridCol;
    }

    /**
     * Ensure all zone rows exist in the mapzone table for every known level.
     * This is idempotent — existing rows are skipped.
     * Called once on TelemetryClient startup.
     *
     * @param db the DatabaseManager to use for connections
     */
    public static void ensureZonesPopulated(DatabaseManager db) {
        if (populated || db == null || !db.isAvailable()) {
            return;
        }

        int tileSize = GameConstants.World.TILE_SIZE; // 64

        try (Connection conn = db.getConnection()) {
            int inserted = 0;

            for (Map.Entry<String, int[]> entry : LEVEL_DIMENSIONS.entrySet()) {
                String levelName = entry.getKey();
                int[] dims = entry.getValue();
                int levelIdx = LEVEL_INDEX.get(levelName);

                int mapHeightPx = dims[0] * tileSize;
                int mapWidthPx  = dims[1] * tileSize;

                int zoneCols = (mapWidthPx  + ZONE_SIZE - 1) / ZONE_SIZE; // ceil division
                int zoneRows = (mapHeightPx + ZONE_SIZE - 1) / ZONE_SIZE;

                for (int r = 0; r < zoneRows; r++) {
                    for (int c = 0; c < zoneCols; c++) {
                        int areaCode = levelIdx * 10_000 + r * 100 + c;

                        // Skip if already present
                        if (zoneExists(conn, areaCode)) {
                            continue;
                        }

                        int xMin = c * ZONE_SIZE;
                        int yMin = r * ZONE_SIZE;
                        int xMax = Math.min(xMin + ZONE_SIZE, mapWidthPx);
                        int yMax = Math.min(yMin + ZONE_SIZE, mapHeightPx);
                        String zoneName = levelName + "_R" + r + "_C" + c;

                        try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT_ZONE)) {
                            ps.setInt(1, areaCode);
                            ps.setString(2, levelName);
                            ps.setString(3, zoneName);
                            ps.setInt(4, xMin);
                            ps.setInt(5, yMin);
                            ps.setInt(6, xMax);
                            ps.setInt(7, yMax);
                            ps.executeUpdate();
                            inserted++;
                        }
                    }
                }
            }

            populated = true;
            if (inserted > 0) {
                logger.info("MapZoneManager: Inserted {} new zone rows", inserted);
            } else {
                logger.debug("MapZoneManager: All zones already present — nothing to insert");
            }

        } catch (SQLException e) {
            logger.warn("MapZoneManager: Failed to populate zones: {}", e.getMessage());
        }
    }

    // ====================================================================
    // INTERNAL
    // ====================================================================

    private static boolean zoneExists(Connection conn, int areaCode) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_ZONE_EXISTS)) {
            ps.setInt(1, areaCode);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
}
