package com.buglife.levels;

/**
 * Configuration for which game mechanics are enabled in a level.
 * 
 * USAGE: In your LevelConfig, return a MechanicsConfig like this:
 * 
 *   return new MechanicsConfig()
 *       .enableDash()           // Player can dash with SHIFT
 *       .enableToy()            // Toy spawns and can be thrown
 *       .enableTripWires()      // TripWires spawn in level
 *       .enableSpeedBoostFood(); // ENERGY_SEED food type spawns
 */
public class MechanicsConfig {
    
    private boolean dashEnabled = false;
    private boolean toyEnabled = false;
    private boolean tripWiresEnabled = false;
    private boolean speedBoostFoodEnabled = false;
    
    // ========== BUILDER METHODS (Chain these!) ==========
    
    /** Enable dash ability (SHIFT key) */
    public MechanicsConfig enableDash() {
        this.dashEnabled = true;
        return this;
    }
    
    /** Enable toy mechanic (pick up with E, throw with F) */
    public MechanicsConfig enableToy() {
        this.toyEnabled = true;
        return this;
    }
    
    /** Enable tripwire traps in level */
    public MechanicsConfig enableTripWires() {
        this.tripWiresEnabled = true;
        return this;
    }
    
    /** Enable ENERGY_SEED food type (gives speed boost) */
    public MechanicsConfig enableSpeedBoostFood() {
        this.speedBoostFoodEnabled = true;
        return this;
    }
    
    // ========== GETTERS ==========
    
    public boolean isDashEnabled() {
        return dashEnabled;
    }
    
    public boolean isToyEnabled() {
        return toyEnabled;
    }
    
    public boolean isTripWiresEnabled() {
        return tripWiresEnabled;
    }
    
    public boolean isSpeedBoostFoodEnabled() {
        return speedBoostFoodEnabled;
    }
}
