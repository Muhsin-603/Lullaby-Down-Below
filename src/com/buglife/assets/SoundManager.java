package com.buglife.assets;

import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.InputStream;
//import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class SoundManager {

    private Map<String, Clip> soundClips; // Stores loaded sound effects
    private float masterVolume = 0.8f; // 0.0 to 1.0
    private float musicVolume = 0.8f;
    private float sfxVolume = 1.0f;
    private final String[] musicTracks = {"music", "menuMusic", "chasing"};

    public SoundManager() {
        soundClips = new HashMap<>();
        // Pre-load sounds you'll use often
        
        loadSound("eat", "/res/sounds/eat_sound.wav");
        loadSound("webbed", "/res/sounds/web_sound.wav");
        loadSound("gameOver", "/res/sounds/humming.wav");
        loadSound("music", "/res/sounds/game_theme.wav");
        loadSound("struggle", "/res/sounds/struggle.wav");
        loadSound("menu", "/res/sounds/menu_selection.wav");
        loadSound("menuMusic", "/res/sounds/menu_music.wav");
        loadSound("lowhunger", "/res/sounds/low_hunger.wav");
        loadSound("chasing", "/res/sounds/chasing.wav");
        // Load others as needed
    }

    public void loadSound(String name, String path) {
        try (InputStream audioSrc = getClass().getResourceAsStream(path);
             InputStream bufferedIn = audioSrc != null ? new BufferedInputStream(audioSrc) : null) {
            
            if (bufferedIn == null) {
                System.err.println("Sound file not found: " + path);
                return;
            }

            AudioInputStream audioStream = AudioSystem.getAudioInputStream(bufferedIn);
            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);
            soundClips.put(name, clip);
            
        } catch (UnsupportedAudioFileException e) {
            System.err.println("Error: Audio file format not supported: " + path + " - Use WAV format.");
        } catch (LineUnavailableException e) {
            System.err.println("Error: Audio line unavailable for: " + path);
        } catch (Exception e) {
            System.err.println("Error loading sound: " + path);
        }
    }

    public void playSound(String name) {
        Clip clip = soundClips.get(name);
        if (clip != null) {
            // Stop and reset the clip before playing again
            if (clip.isRunning()) {
                clip.stop();
            }
            clip.setFramePosition(0); // Rewind to the beginning
            clip.start();
        } else {
            System.err.println("Sound not found: " + name);
        }
    }

    // Special method for looping background music
    public void loopSound(String name) {
        Clip clip = soundClips.get(name);
        if (clip != null) {
            clip.loop(Clip.LOOP_CONTINUOUSLY);
        } else {
            System.err.println("Looping sound not found: " + name);
            // Try loading it now if not pre-loaded
            // loadSound(name, "/res/sounds/" + name + ".wav"); // Adjust path as needed
            // clip = soundClips.get(name);
            // if (clip != null) clip.loop(Clip.LOOP_CONTINUOUSLY);
        }
    }
    
    // Method to stop a specific sound or all sounds
    public void stopSound(String name) {
         Clip clip = soundClips.get(name);
         if (clip != null && clip.isRunning()) {
             clip.stop();
         }
    }

    public void stopAllSounds() {
        for (Clip clip : soundClips.values()) {
            if (clip.isRunning()) {
                clip.stop();
            }
        }
    }
    
    /**
     * Set volume for a specific sound (0.0 to 1.0)
     */
    public void setVolume(String soundName, float volume) {
        Clip clip = soundClips.get(soundName);
        if (clip != null) {
            setClipVolume(clip, volume * masterVolume);
        }
    }
    
    /**
     * Set master volume affecting all sounds (0.0 to 1.0)
     */
    public void setMasterVolume(float volume) {
        this.masterVolume = Math.max(0.0f, Math.min(1.0f, volume));
        updateAllVolumes();
    }
    
    /**
     * Set music volume for background music tracks (0.0 to 1.0)
     */
    public void setMusicVolume(float volume) {
        this.musicVolume = Math.max(0.0f, Math.min(1.0f, volume));
        for (String musicName : musicTracks) {
            Clip clip = soundClips.get(musicName);
            if (clip != null) {
                setClipVolume(clip, musicVolume * masterVolume);
            }
        }
    }
    
    /**
     * Set SFX volume for sound effects (0.0 to 1.0)
     */
    public void setSFXVolume(float volume) {
        this.sfxVolume = Math.max(0.0f, Math.min(1.0f, volume));
        for (Map.Entry<String, Clip> entry : soundClips.entrySet()) {
            boolean isMusic = false;
            for (String musicName : musicTracks) {
                if (entry.getKey().equals(musicName)) {
                    isMusic = true;
                    break;
                }
            }
            if (!isMusic) {
                setClipVolume(entry.getValue(), sfxVolume * masterVolume);
            }
        }
    }
    
    /**
     * Update all volumes based on current settings
     */
    private void updateAllVolumes() {
        for (Map.Entry<String, Clip> entry : soundClips.entrySet()) {
            boolean isMusic = false;
            for (String musicName : musicTracks) {
                if (entry.getKey().equals(musicName)) {
                    isMusic = true;
                    break;
                }
            }
            float volume = isMusic ? musicVolume : sfxVolume;
            setClipVolume(entry.getValue(), volume * masterVolume);
        }
    }
    
    /**
     * Set volume on a clip using FloatControl (converts 0-1 range to decibel range)
     */
    private void setClipVolume(Clip clip, float volume) {
        if (clip == null) return;
        
        try {
            FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            float min = gainControl.getMinimum(); // typically -80.0 dB
            float max = gainControl.getMaximum(); // typically 6.0 dB
            
            // Convert linear 0-1 to logarithmic decibel scale
            float gain;
            if (volume <= 0.0f) {
                gain = min;
            } else {
                // Logarithmic mapping for natural volume perception
                gain = min + (max - min) * (float) (Math.log10(volume * 9 + 1) / Math.log10(10));
            }
            
            gainControl.setValue(gain);
        } catch (IllegalArgumentException e) {
            System.err.println("Volume control not supported for this clip");
        }
    }
    
    public float getMasterVolume() {
        return masterVolume;
    }
    
    public float getMusicVolume() {
        return musicVolume;
    }
    
    public float getSFXVolume() {
        return sfxVolume;
    }
}