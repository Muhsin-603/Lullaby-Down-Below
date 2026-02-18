package com.buglife.save;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * CloudSaveManager — The Cloud Backup (Tier 2).
 * 
 * Five seconds after a local save finishes, this quietly packages the JSON
 * and mails it to the Overseer via HTTP. If the Overseer is offline, it fails
 * silently — the local save already has the player covered.
 * 
 * The Magic Trick: If a player logs in on a new machine with no local save,
 * this asks the Overseer for their cloud save and downloads it.
 * Cross-progression, arcade style.
 * 
 * Endpoints:
 *   POST /user/register     — Register or welcome back a player
 *   POST /save/upload       — Upload a save file to the cloud
 *   POST /save/download     — Download a cloud save for a player
 */
public class CloudSaveManager {
    private static final Logger logger = LoggerFactory.getLogger(CloudSaveManager.class);

    private static final String OVERSEER_HOST = "http://127.0.0.1:8090";
    private static final int TIMEOUT_MS = 3000;
    private static final int CLOUD_SAVE_DELAY_MS = 5000; // 5 second delay before cloud upload

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * The Overseer Registration — Send a whisper to /user/register.
     * If the player is new, the Overseer creates a fresh file.
     * If they exist, it welcomes them back.
     * 
     * @param playerName The arcade identity
     * @return true if the Overseer acknowledged, false if offline/error
     */
    public static boolean registerWithOverseer(String playerName) {
        String payload = String.format(
            "{\"user_id\":\"%s\",\"os_info\":\"%s %s\"}",
            playerName.toUpperCase(),
            System.getProperty("os.name"),
            System.getProperty("os.version")
        );

        try {
            String response = sendSync("/user/register", payload);
            if (response != null) {
                logger.info("Overseer registration successful for: {}", playerName);
                return true;
            }
        } catch (Exception e) {
            logger.warn("Overseer registration failed (offline?): {}", e.getMessage());
        }
        return false;
    }

    /**
     * Register with the Overseer asynchronously (non-blocking).
     */
    public static void registerWithOverseerAsync(String playerName) {
        executor.submit(() -> registerWithOverseer(playerName));
    }

    /**
     * Upload a save to the cloud — delayed by 5 seconds after local save.
     * Runs entirely in the background. Fire and forget.
     * 
     * @param saveData The save snapshot to upload
     */
    public static void uploadSaveAsync(SaveData saveData) {
        executor.submit(() -> {
            try {
                // The 5-second delay — let the player breathe before we phone home
                Thread.sleep(CLOUD_SAVE_DELAY_MS);

                String saveJson = mapper.writeValueAsString(saveData);
                String payload = String.format(
                    "{\"user_id\":\"%s\",\"save_data\":%s}",
                    saveData.getPlayerName().toUpperCase(),
                    saveJson
                );

                String response = sendSync("/save/upload", payload);
                if (response != null) {
                    logger.info("Cloud save uploaded for: {}", saveData.getPlayerName());
                } else {
                    logger.warn("Cloud save upload failed for: {} (Overseer may be offline)", 
                            saveData.getPlayerName());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Cloud save upload interrupted for: {}", saveData.getPlayerName());
            } catch (Exception e) {
                logger.error("Cloud save upload error for: {}", saveData.getPlayerName(), e);
            }
        });
    }

    /**
     * Download a cloud save — The Magic Trick.
     * "I don't have a local file for SHIBILI. Let me ask the Overseer."
     * 
     * This runs SYNCHRONOUSLY because we need the result to load the game.
     * 
     * @param playerName The player to look up
     * @return SaveData from the cloud, or null if not found/offline
     */
    public static SaveData downloadSave(String playerName) {
        String payload = String.format(
            "{\"user_id\":\"%s\"}",
            playerName.toUpperCase()
        );

        try {
            String response = sendSync("/save/download", payload);
            if (response != null && !response.isEmpty()) {
                // Parse the Overseer's response — expects { "save_data": {...} }
                JsonNode root = mapper.readTree(response);
                JsonNode saveNode = root.has("save_data") ? root.get("save_data") : root;
                
                SaveData saveData = mapper.treeToValue(saveNode, SaveData.class);
                logger.info("Cloud save downloaded for: {} (Level: {})", 
                        playerName, saveData.getLevelId());
                return saveData;
            }
        } catch (Exception e) {
            logger.warn("Cloud save download failed for: {} — {}", playerName, e.getMessage());
        }
        return null;
    }

    /**
     * Attempt to download a cloud save asynchronously with a timeout.
     * Used during login when checking for cross-machine saves.
     */
    public static SaveData downloadSaveWithTimeout(String playerName, int timeoutSeconds) {
        Future<SaveData> future = executor.submit(() -> downloadSave(playerName));
        try {
            return future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.warn("Cloud save download timed out for: {}", playerName);
            future.cancel(true);
            return null;
        }
    }

    /**
     * Shutdown the cloud executor gracefully.
     */
    public static void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(3, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        logger.info("CloudSaveManager shut down");
    }

    // ─── Phase 1: The Guest List (Pre-Fetch) ───────────────────

    /**
     * Fetch the complete list of all player names from the Overseer.
     * Endpoint: GET /users/list
     * 
     * The Overseer opens the MySQL vault, looks at the users table,
     * and compiles a neat JSON list of every name that has ever played.
     * 
     * @return List of player names, or empty list if Overseer is offline
     */
    public static java.util.List<String> fetchUserList() {
        java.util.List<String> users = new java.util.ArrayList<>();
        try {
            String response = sendGetSync("/users/list");
            if (response != null && !response.isEmpty()) {
                JsonNode root = mapper.readTree(response);
                JsonNode usersNode = root.has("users") ? root.get("users") : root;
                
                if (usersNode.isArray()) {
                    for (JsonNode node : usersNode) {
                        if (node.isTextual()) {
                            users.add(node.asText());
                        } else if (node.has("user_id")) {
                            users.add(node.get("user_id").asText());
                        } else if (node.has("username")) {
                            users.add(node.get("username").asText());
                        }
                    }
                }
                logger.info("Fetched {} users from Overseer", users.size());
            }
        } catch (Exception e) {
            logger.warn("Failed to fetch user list from Overseer: {}", e.getMessage());
        }
        return users;
    }

    /**
     * Fetch user list asynchronously with a timeout.
     */
    public static java.util.List<String> fetchUserListWithTimeout(int timeoutSeconds) {
        Future<java.util.List<String>> future = executor.submit(() -> fetchUserList());
        try {
            return future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.warn("User list fetch timed out after {}s", timeoutSeconds);
            future.cancel(true);
            return new java.util.ArrayList<>();
        }
    }

    // ─── Phase 3: The Hall of Infamy (Leaderboard) ──────────────

    /**
     * A single leaderboard entry from the Overseer.
     */
    public static class LeaderboardEntry {
        private String playerName;
        private int rank;
        private int value;       // The stat value (deaths, playtime, etc.)
        private String category; // What are we ranking by

        public LeaderboardEntry(String playerName, int rank, int value, String category) {
            this.playerName = playerName;
            this.rank = rank;
            this.value = value;
            this.category = category;
        }

        public String getPlayerName() { return playerName; }
        public int getRank() { return rank; }
        public int getValue() { return value; }
        public String getCategory() { return category; }
    }

    /**
     * Fetch the leaderboard from the Overseer.
     * Endpoint: GET /leaderboard?category=deaths&limit=10
     * 
     * The Overseer does the heavy lifting — SQL sorts, Java just draws.
     * 
     * @param category What to rank by: "deaths", "playtime", "levels_completed"
     * @param limit    How many entries (top N)
     * @return Sorted list of leaderboard entries, empty if offline
     */
    public static java.util.List<LeaderboardEntry> fetchLeaderboard(String category, int limit) {
        java.util.List<LeaderboardEntry> entries = new java.util.ArrayList<>();
        try {
            String endpoint = String.format("/leaderboard?category=%s&limit=%d", category, limit);
            String response = sendGetSync(endpoint);
            if (response != null && !response.isEmpty()) {
                JsonNode root = mapper.readTree(response);
                JsonNode leaderboard = root.has("leaderboard") ? root.get("leaderboard") : root;
                
                if (leaderboard.isArray()) {
                    int rank = 1;
                    for (JsonNode entry : leaderboard) {
                        String name = entry.has("user_id") ? entry.get("user_id").asText() 
                                    : entry.has("player_name") ? entry.get("player_name").asText() 
                                    : "???";
                        int value = entry.has("value") ? entry.get("value").asInt()
                                  : entry.has("count") ? entry.get("count").asInt()
                                  : entry.has("total") ? entry.get("total").asInt()
                                  : 0;
                        entries.add(new LeaderboardEntry(name, rank++, value, category));
                    }
                }
                logger.info("Fetched leaderboard: {} entries for category '{}'", entries.size(), category);
            }
        } catch (Exception e) {
            logger.warn("Failed to fetch leaderboard from Overseer: {}", e.getMessage());
        }
        return entries;
    }

    /**
     * Fetch leaderboard asynchronously with timeout.
     */
    public static java.util.List<LeaderboardEntry> fetchLeaderboardWithTimeout(
            String category, int limit, int timeoutSeconds) {
        Future<java.util.List<LeaderboardEntry>> future = 
                executor.submit(() -> fetchLeaderboard(category, limit));
        try {
            return future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.warn("Leaderboard fetch timed out after {}s", timeoutSeconds);
            future.cancel(true);
            return new java.util.ArrayList<>();
        }
    }

    // ─── HTTP Plumbing ────────────────────────────────────────────

    /**
     * Send a GET request to the Overseer (for user list, leaderboard).
     */
    private static String sendGetSync(String endpoint) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(OVERSEER_HOST + endpoint);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);

            int responseCode = conn.getResponseCode();
            
            if (responseCode >= 200 && responseCode < 300) {
                StringBuilder response = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                }
                return response.toString();
            } else {
                logger.warn("Overseer GET returned HTTP {}: {}", responseCode, endpoint);
                return null;
            }
        } catch (Exception e) {
            logger.debug("Overseer unreachable for GET {}{}: {}", OVERSEER_HOST, endpoint, e.getMessage());
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static String sendSync(String endpoint, String jsonPayload) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(OVERSEER_HOST + endpoint);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            
            if (responseCode >= 200 && responseCode < 300) {
                // Read response body
                StringBuilder response = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                }
                return response.toString();
            } else {
                logger.warn("Overseer returned HTTP {}: {}", responseCode, endpoint);
                return null;
            }
        } catch (Exception e) {
            logger.debug("Overseer unreachable at {}{}: {}", OVERSEER_HOST, endpoint, e.getMessage());
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }
}
