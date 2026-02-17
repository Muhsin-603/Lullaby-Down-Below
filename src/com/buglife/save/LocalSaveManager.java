package com.buglife.save;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * LocalSaveManager — The Local Memory Card (Tier 1).
 * 
 * Whenever the player saves, this writes the JSON photograph INSTANTLY
 * to a local file on their hard drive: saves/PLAYERNAME.sav
 * 
 * This guarantees instant load times and complete offline functionality.
 * The arcade cabinet doesn't need Wi-Fi to remember your high score.
 */
public class LocalSaveManager {
    private static final Logger logger = LoggerFactory.getLogger(LocalSaveManager.class);

    private static final String SAVES_DIR = "saves";
    private static final String SAVE_EXTENSION = ".sav";
    private static final ObjectMapper mapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    /**
     * Save the game state to a local file.
     * File: saves/PLAYERNAME.sav
     * 
     * @param saveData The frozen snapshot of the player's world
     * @return true if save succeeded, false otherwise
     */
    public static boolean save(SaveData saveData) {
        if (saveData.getPlayerName() == null || saveData.getPlayerName().isEmpty()) {
            logger.error("Cannot save: No player name set in SaveData");
            return false;
        }

        UserProfile.ensureSavesDirectory();
        String filename = saveData.getPlayerName().toUpperCase() + SAVE_EXTENSION;
        File saveFile = Paths.get(SAVES_DIR, filename).toFile();

        try {
            mapper.writeValue(saveFile, saveData);
            logger.info("Local save written: {} (Level: {}, Hunger: {})", 
                    filename, saveData.getLevelId(), saveData.getHunger());
            return true;
        } catch (IOException e) {
            logger.error("Failed to write local save: {}", filename, e);
            return false;
        }
    }

    /**
     * Load a save file from disk for the given player.
     * 
     * @param playerName The arcade identity to look up
     * @return The SaveData if found, null if no local save exists
     */
    public static SaveData load(String playerName) {
        if (playerName == null || playerName.isEmpty()) {
            logger.error("Cannot load: No player name provided");
            return null;
        }

        String filename = playerName.toUpperCase() + SAVE_EXTENSION;
        File saveFile = Paths.get(SAVES_DIR, filename).toFile();

        if (!saveFile.exists()) {
            logger.info("No local save found for player: {}", playerName);
            return null;
        }

        try {
            SaveData saveData = mapper.readValue(saveFile, SaveData.class);
            logger.info("Local save loaded for {}: Level={}, Pos=({},{}), Hunger={}", 
                    playerName, saveData.getLevelId(), 
                    (int) saveData.getPlayerX(), (int) saveData.getPlayerY(),
                    saveData.getHunger());
            return saveData;
        } catch (IOException e) {
            logger.error("Failed to read local save for: {}", playerName, e);
            return null;
        }
    }

    /**
     * Check if a local save exists for the given player.
     */
    public static boolean hasSave(String playerName) {
        if (playerName == null || playerName.isEmpty()) return false;
        String filename = playerName.toUpperCase() + SAVE_EXTENSION;
        return Paths.get(SAVES_DIR, filename).toFile().exists();
    }

    /**
     * Delete a local save (for "New Game" overwriting).
     */
    public static boolean deleteSave(String playerName) {
        if (playerName == null || playerName.isEmpty()) return false;
        
        String filename = playerName.toUpperCase() + SAVE_EXTENSION;
        Path savePath = Paths.get(SAVES_DIR, filename);

        try {
            if (Files.exists(savePath)) {
                Files.delete(savePath);
                logger.info("Deleted local save: {}", filename);
                return true;
            }
            return false;
        } catch (IOException e) {
            logger.error("Failed to delete save: {}", filename, e);
            return false;
        }
    }

    /**
     * Get the raw JSON string of a save file (for cloud upload payloads).
     */
    public static String getSaveAsJson(String playerName) {
        if (playerName == null || playerName.isEmpty()) return null;

        String filename = playerName.toUpperCase() + SAVE_EXTENSION;
        File saveFile = Paths.get(SAVES_DIR, filename).toFile();

        if (!saveFile.exists()) return null;

        try {
            return new String(Files.readAllBytes(saveFile.toPath()));
        } catch (IOException e) {
            logger.error("Failed to read save as JSON: {}", filename, e);
            return null;
        }
    }
}
