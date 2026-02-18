package com.buglife.save;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.buglife.entities.Food;
import com.buglife.entities.Player;
import com.buglife.entities.Toy;

/**
 * SaveManager — The Two-Tiered Vault Orchestrator.
 * 
 * This is the single entry point for all save/load operations. It coordinates
 * the Local Memory Card (Tier 1) and Cloud Backup (Tier 2) to provide:
 * 
 *   1. Instant local saves (always works, even offline)
 *   2. Delayed cloud uploads (5 seconds after local save)
 *   3. Cloud download fallback (cross-progression magic)
 * 
 * Save triggers:
 *   - Auto-Save: When player steps on ladder (level complete)
 *   - Rage Quit: When player hits "Quit to Menu" from pause
 *   - Manual:    Future — save tiles on the map
 */
public class SaveManager {
    private static final Logger logger = LoggerFactory.getLogger(SaveManager.class);

    /**
     * Take the photograph: Capture the current game state into a SaveData snapshot.
     * 
     * @param player       The player entity
     * @param currentLevel Current level ID (e.g. "level3")
     * @param levelIndex   Current level index in progression
     * @param foods        Remaining food items on the map
     * @param toy          The toy entity (can be null)
     * @return A frozen SaveData snapshot
     */
    public static SaveData captureState(Player player, String currentLevel, int levelIndex,
                                         List<Food> foods, Toy toy) {
        SaveData save = new SaveData();

        // The WHERE
        save.setLevelId(currentLevel);
        save.setLevelIndex(levelIndex);
        save.setPlayerX(player.getX());
        save.setPlayerY(player.getY());
        save.setFacingDirection(player.getFacingDirection());

        // The BODY
        save.setHunger(player.getHunger());
        save.setCrying(player.isCrying());
        save.setSpeedBoostTimer(player.getSpeedBoostTimer());

        // The POCKETS
        if (toy != null) {
            save.setHasToy(toy.isCarried());
            if (!toy.isCarried()) {
                save.setToyX(toy.getCenterX());
                save.setToyY(toy.getCenterY());
            }
        }

        // The WORLD STATE (cruel)
        List<SaveData.FoodState> foodStates = new ArrayList<>();
        if (foods != null) {
            for (Food food : foods) {
                foodStates.add(new SaveData.FoodState(
                    food.getCenterX(), food.getCenterY(), food.getType().name()
                ));
            }
        }
        save.setRemainingFoods(foodStates);

        // IT IS TIME (TRACKING CAREER)
        save.setTotalPlaytimeSeconds(0);

        // META
        save.setPlayerName(UserProfile.getActivePlayer());
        save.setSaveTimestamp(System.currentTimeMillis());

        return save;
    }

    /**
     * Save the game — The Two-Tiered Vault in action.
     * 
     * 1. Instantly writes to local file (Tier 1)
     * 2. Schedules cloud upload after 5-second delay (Tier 2)
     * 
     * @return true if local save succeeded (cloud is fire-and-forget)
     */
    public static boolean saveGame(Player player, String currentLevel, int levelIndex,
                                    List<Food> foods, Toy toy) {
        String playerName = UserProfile.getActivePlayer();
        if (playerName == null) {
            logger.error("Cannot save: No active player identified");
            return false;
        }

        // Take the photograph
        SaveData saveData = captureState(player, currentLevel, levelIndex, foods, toy);

        // Tier 1: Local Memory Card — instant write
        boolean localSuccess = LocalSaveManager.save(saveData);

        if (localSuccess) {
            logger.info("Game saved locally for {} on {}", playerName, currentLevel);

            // Tier 2: Cloud Backup — delayed upload (fire and forget)
            CloudSaveManager.uploadSaveAsync(saveData);
        }

        return localSuccess;
    }

    /**
     * Load a save — The Two-Tiered retrieval.
     * 
     * 1. Check local disk first (fast)
     * 2. If no local save, ask the Overseer (cross-progression magic)
     * 3. If cloud save found, write it locally for next time
     * 
     * @param playerName The arcade identity
     * @return SaveData if found, null if no save exists anywhere
     */
    public static SaveData loadGame(String playerName) {
        if (playerName == null || playerName.isEmpty()) {
            logger.error("Cannot load: No player name provided");
            return null;
        }

        // Tier 1: Check local disk
        SaveData localSave = LocalSaveManager.load(playerName);
        if (localSave != null) {
            logger.info("Loaded local save for {}: Level={}", playerName, localSave.getLevelId());
            return localSave;
        }

        // Tier 2: "I don't have a local file. Let me ask the Overseer."
        logger.info("No local save for {}. Checking cloud...", playerName);
        SaveData cloudSave = CloudSaveManager.downloadSaveWithTimeout(playerName, 5);

        if (cloudSave != null) {
            // Cache the cloud save locally for next time
            LocalSaveManager.save(cloudSave);
            logger.info("Cloud save downloaded and cached locally for {}", playerName);
            return cloudSave;
        }

        logger.info("No save found anywhere for {}", playerName);
        return null;
    }

    /**
     * Check if any save exists for this player (local or cloud).
     * Quick local check only — doesn't hit the network.
     */
    public static boolean hasSave(String playerName) {
        return LocalSaveManager.hasSave(playerName);
    }

    /**
     * Delete saves for a player (when starting a truly new game).
     */
    public static void deleteSave(String playerName) {
        LocalSaveManager.deleteSave(playerName);
        logger.info("Save deleted for {}", playerName);
    }

    /**
     * Shutdown cloud services gracefully.
     */
    public static void shutdown() {
        CloudSaveManager.shutdown();
    }
}
