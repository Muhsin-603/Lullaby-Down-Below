package com.buglife.assets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Centralized asset management system for images and sounds.
 * Handles loading, caching, and lifecycle management of game assets.
 */
public class AssetManager {
    private static final Logger logger = LoggerFactory.getLogger(AssetManager.class);
    private static AssetManager instance;
    
    private final Map<String, BufferedImage> imageCache;
    private final Map<String, Clip> soundCache;
    private final Map<String, Integer> referenceCount;
    
    private AssetManager() {
        this.imageCache = new HashMap<>();
        this.soundCache = new HashMap<>();
        this.referenceCount = new HashMap<>();
    }
    
    public static AssetManager getInstance() {
        if (instance == null) {
            instance = new AssetManager();
        }
        return instance;
    }
    
    /**
     * Load an image from resources. Returns cached version if already loaded.
     */
    public BufferedImage loadImage(String path) {
        if (imageCache.containsKey(path)) {
            incrementReference(path);
            return imageCache.get(path);
        }
        
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) {
                logger.error("Image not found: {}", path);
                return createErrorImage();
            }
            
            BufferedImage image = ImageIO.read(is);
            imageCache.put(path, image);
            referenceCount.put(path, 1);
            logger.debug("Loaded image: {}", path);
            return image;
            
        } catch (IOException e) {
            logger.error("Failed to load image: " + path, e);
            return createErrorImage();
        }
    }
    
    /**
     * Load multiple images at once (for sprite sheets, animations, etc.)
     */
    public Map<String, BufferedImage> loadImages(String... paths) {
        Map<String, BufferedImage> images = new HashMap<>();
        for (String path : paths) {
            images.put(path, loadImage(path));
        }
        return images;
    }
    
    /**
     * Load a sound clip from resources. Returns cached version if already loaded.
     */
    public Clip loadSound(String path) {
        if (soundCache.containsKey(path)) {
            incrementReference(path);
            return soundCache.get(path);
        }
        
        try (InputStream audioSrc = getClass().getResourceAsStream(path);
             InputStream bufferedIn = audioSrc != null ? new BufferedInputStream(audioSrc) : null) {
            
            if (bufferedIn == null) {
                logger.error("Sound file not found: {}", path);
                return null;
            }
            
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(bufferedIn);
            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);
            
            soundCache.put(path, clip);
            referenceCount.put(path, 1);
            logger.debug("Loaded sound: {}", path);
            return clip;
            
        } catch (UnsupportedAudioFileException e) {
            logger.error("Unsupported audio format: " + path, e);
        } catch (LineUnavailableException e) {
            logger.error("Audio line unavailable: " + path, e);
        } catch (Exception e) {
            logger.error("Failed to load sound: " + path, e);
        }
        
        return null;
    }
    
    /**
     * Release an asset (decrements reference count, disposes if reaches 0)
     */
    public void releaseAsset(String path) {
        Integer count = referenceCount.get(path);
        if (count == null || count <= 0) {
            return;
        }
        
        count--;
        if (count == 0) {
            // Actually dispose the asset
            referenceCount.remove(path);
            
            if (soundCache.containsKey(path)) {
                Clip clip = soundCache.remove(path);
                if (clip != null && clip.isOpen()) {
                    clip.close();
                }
                logger.debug("Disposed sound: {}", path);
            }
            
            if (imageCache.containsKey(path)) {
                imageCache.remove(path);
                logger.debug("Disposed image: {}", path);
            }
        } else {
            referenceCount.put(path, count);
        }
    }
    
    /**
     * Get a cached image without loading
     */
    public BufferedImage getImage(String path) {
        return imageCache.get(path);
    }
    
    /**
     * Get a cached sound without loading
     */
    public Clip getSound(String path) {
        return soundCache.get(path);
    }
    
    /**
     * Check if an asset is loaded
     */
    public boolean isLoaded(String path) {
        return imageCache.containsKey(path) || soundCache.containsKey(path);
    }
    
    /**
     * Dispose all assets and clear caches
     */
    public void disposeAll() {
        // Stop and close all sound clips
        for (Map.Entry<String, Clip> entry : soundCache.entrySet()) {
            Clip clip = entry.getValue();
            if (clip != null) {
                if (clip.isRunning()) {
                    clip.stop();
                }
                if (clip.isOpen()) {
                    clip.close();
                }
            }
        }
        
        soundCache.clear();
        imageCache.clear();
        referenceCount.clear();
        
        logger.info("All assets disposed");
    }
    
    /**
     * Get memory usage statistics
     */
    public String getMemoryStats() {
        return String.format("Images: %d, Sounds: %d, Total references: %d",
                imageCache.size(), soundCache.size(), 
                referenceCount.values().stream().mapToInt(Integer::intValue).sum());
    }
    
    /**
     * Preload common assets during game startup
     */
    public void preloadCommonAssets() {
        logger.info("Preloading common assets...");
        
        // Preload player sprites
        loadImage("/res/sprites/player/pla.png");
        loadImage("/res/sprites/player/webbed_state.png");
        
        // Preload spider sprites
        loadImage("/res/sprites/spider/Walk_0001.png");
        loadImage("/res/sprites/spider/Walk_0002.png");
        
        // Preload common sounds
        loadSound("/res/sounds/game_theme.wav");
        loadSound("/res/sounds/menu_music.wav");
        loadSound("/res/sounds/eat_sound.wav");
        loadSound("/res/sounds/web_sound.wav");
        loadSound("/res/sounds/struggle.wav");
        
        logger.info("Common assets preloaded: {}", getMemoryStats());
    }
    
    private void incrementReference(String path) {
        referenceCount.put(path, referenceCount.getOrDefault(path, 0) + 1);
    }
    
    private BufferedImage createErrorImage() {
        // Create a magenta 32x32 error placeholder
        BufferedImage error = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 32; y++) {
            for (int x = 0; x < 32; x++) {
                error.setRGB(x, y, 0xFFFF00FF); // Magenta
            }
        }
        return error;
    }
}
