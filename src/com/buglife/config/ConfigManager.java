package com.buglife.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Centralized configuration manager for loading and accessing game settings.
 * Uses Jackson for JSON parsing and provides type-safe access to config values.
 */
public class ConfigManager {
    private static final Logger logger = LoggerFactory.getLogger(ConfigManager.class);
    private static ConfigManager instance;
    
    private final ObjectMapper mapper;
    private JsonNode gameConfig;
    private JsonNode keybindings;
    private final Map<String, Object> cache;
    
    private ConfigManager() {
        this.mapper = new ObjectMapper();
        this.cache = new HashMap<>();
        loadConfigurations();
    }
    
    public static ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }
    
    /**
     * Load all configuration files
     */
    private void loadConfigurations() {
        try {
            gameConfig = loadJsonResource("/config.json");
            keybindings = loadJsonResource("/keybindings.json");
            logger.info("Configuration files loaded successfully");
        } catch (IOException e) {
            logger.error("Failed to load configuration files", e);
            // Use default values
            gameConfig = mapper.createObjectNode();
            keybindings = mapper.createObjectNode();
        }
    }
    
    /**
     * Load a JSON resource file
     */
    private JsonNode loadJsonResource(String resourcePath) throws IOException {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }
            return mapper.readTree(is);
        }
    }
    
    /**
     * Reload configurations from disk
     */
    public void reload() {
        cache.clear();
        loadConfigurations();
        logger.info("Configuration reloaded");
    }
    
    /**
     * Get a configuration value by path (e.g., "player.normalSpeed")
     */
    public <T> T get(String path, Class<T> type, T defaultValue) {
        String cacheKey = path + ":" + type.getName();
        
        if (cache.containsKey(cacheKey)) {
            return type.cast(cache.get(cacheKey));
        }
        
        try {
            JsonNode node = getNodeByPath(gameConfig, path);
            if (node == null || node.isMissingNode()) {
                logger.warn("Config path not found: {}, using default: {}", path, defaultValue);
                return defaultValue;
            }
            
            T value = mapper.treeToValue(node, type);
            cache.put(cacheKey, value);
            return value;
        } catch (Exception e) {
            logger.error("Error reading config path: " + path, e);
            return defaultValue;
        }
    }
    
    /**
     * Get an integer configuration value
     */
    public int getInt(String path, int defaultValue) {
        return get(path, Integer.class, defaultValue);
    }
    
    /**
     * Get a double configuration value
     */
    public double getDouble(String path, double defaultValue) {
        return get(path, Double.class, defaultValue);
    }
    
    /**
     * Get a boolean configuration value
     */
    public boolean getBoolean(String path, boolean defaultValue) {
        return get(path, Boolean.class, defaultValue);
    }
    
    /**
     * Get a string configuration value
     */
    public String getString(String path, String defaultValue) {
        return get(path, String.class, defaultValue);
    }
    
    /**
     * Get a keybinding by action name
     */
    public String getKeybinding(String action, String defaultKey) {
        try {
            JsonNode bindingsNode = keybindings.get("keybindings");
            if (bindingsNode != null && bindingsNode.has(action)) {
                return bindingsNode.get(action).asText();
            }
            
            // Check alternate keys
            JsonNode alternateNode = keybindings.get("alternateKeys");
            if (alternateNode != null && alternateNode.has(action)) {
                return alternateNode.get(action).asText();
            }
        } catch (Exception e) {
            logger.error("Error reading keybinding: " + action, e);
        }
        
        return defaultKey;
    }
    
    /**
     * Navigate JSON path (dot-separated)
     */
    private JsonNode getNodeByPath(JsonNode root, String path) {
        if (root == null || path == null || path.isEmpty()) {
            return null;
        }
        
        String[] parts = path.split("\\.");
        JsonNode current = root;
        
        for (String part : parts) {
            if (current == null || !current.has(part)) {
                return null;
            }
            current = current.get(part);
        }
        
        return current;
    }
    
    /**
     * Set a runtime configuration value (not persisted)
     */
    public void setRuntimeValue(String key, Object value) {
        cache.put(key, value);
    }
    
    /**
     * Get a runtime configuration value
     */
    @SuppressWarnings("unchecked")
    public <T> T getRuntimeValue(String key, T defaultValue) {
        Object value = cache.get(key);
        if (value == null) {
            return defaultValue;
        }
        return (T) value;
    }
}
