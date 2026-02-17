package com.buglife.save;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * UserProfile — The Gatekeeper's Ledger.
 * 
 * Manages player identity for the arcade "hotseat" system. When Lullaby Down Below
 * boots, a player must IDENTIFY YOURSELF. This class checks the local hidden registry
 * to see if they've played on this machine before, and coordinates with the Overseer
 * for cross-machine recognition.
 * 
 * Local profiles are stored in: saves/.profiles.json (hidden intent, visible file)
 */
public class UserProfile {
    private static final Logger logger = LoggerFactory.getLogger(UserProfile.class);

    private static final String SAVES_DIR = "saves";
    private static final String PROFILES_FILE = ".profiles.json";
    private static final ObjectMapper mapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    // The currently active player name — locked in after identification
    private static String activePlayerName = null;

    /**
     * The profile registry — a simple list of names who have played on this machine.
     */
    public static class ProfileRegistry {
        private List<String> knownPlayers = new ArrayList<>();

        public ProfileRegistry() {}

        public List<String> getKnownPlayers() { return knownPlayers; }
        public void setKnownPlayers(List<String> knownPlayers) { this.knownPlayers = knownPlayers; }

        public boolean hasPlayer(String name) {
            return knownPlayers.stream()
                    .anyMatch(p -> p.equalsIgnoreCase(name));
        }

        public void addPlayer(String name) {
            if (!hasPlayer(name)) {
                knownPlayers.add(name.toUpperCase());
            }
        }
    }

    /**
     * Initialize the saves directory and profiles file.
     */
    public static void ensureSavesDirectory() {
        try {
            Path savesPath = Paths.get(SAVES_DIR);
            if (!Files.exists(savesPath)) {
                Files.createDirectories(savesPath);
                logger.info("Created saves directory: {}", savesPath.toAbsolutePath());
            }
        } catch (IOException e) {
            logger.error("Failed to create saves directory", e);
        }
    }

    /**
     * Load the profile registry from disk.
     * Returns empty registry if file doesn't exist yet.
     */
    public static ProfileRegistry loadRegistry() {
        ensureSavesDirectory();
        File file = Paths.get(SAVES_DIR, PROFILES_FILE).toFile();

        if (!file.exists()) {
            logger.info("No profile registry found — fresh machine");
            return new ProfileRegistry();
        }

        try {
            ProfileRegistry registry = mapper.readValue(file, ProfileRegistry.class);
            logger.info("Loaded profile registry with {} known players", registry.getKnownPlayers().size());
            return registry;
        } catch (IOException e) {
            logger.error("Failed to read profile registry, starting fresh", e);
            return new ProfileRegistry();
        }
    }

    /**
     * Save the profile registry back to disk.
     */
    public static void saveRegistry(ProfileRegistry registry) {
        ensureSavesDirectory();
        File file = Paths.get(SAVES_DIR, PROFILES_FILE).toFile();

        try {
            mapper.writeValue(file, registry);
            logger.info("Saved profile registry with {} players", registry.getKnownPlayers().size());
        } catch (IOException e) {
            logger.error("Failed to save profile registry", e);
        }
    }

    /**
     * The Brain Check: Is this player known on this machine?
     */
    public static boolean isKnownLocally(String playerName) {
        ProfileRegistry registry = loadRegistry();
        return registry.hasPlayer(playerName);
    }

    /**
     * Register a player locally after successful identification.
     */
    public static void registerLocally(String playerName) {
        ProfileRegistry registry = loadRegistry();
        registry.addPlayer(playerName);
        saveRegistry(registry);
        logger.info("Registered player locally: {}", playerName);
    }

    /**
     * Lock a player name into the active session.
     * This is "The Handoff" — every event from now on is stained with this name.
     */
    public static void setActivePlayer(String playerName) {
        activePlayerName = playerName.toUpperCase();
        logger.info("Active player locked in: {}", activePlayerName);
    }

    /**
     * Get the currently active player name.
     */
    public static String getActivePlayer() {
        return activePlayerName;
    }

    /**
     * Check if a player is currently identified.
     */
    public static boolean hasActivePlayer() {
        return activePlayerName != null && !activePlayerName.isEmpty();
    }

    /**
     * Get all known player names for this machine (for "Continue as..." prompts).
     */
    public static List<String> getKnownPlayers() {
        return loadRegistry().getKnownPlayers();
    }
}
