package com.buglife.states;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;

import com.buglife.assets.SoundManager;
import com.buglife.entities.Food;
import com.buglife.entities.Player;
import com.buglife.entities.Snail;
import com.buglife.utils.PerformanceMonitor;
import com.buglife.save.SaveData;
import com.buglife.save.SaveManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.buglife.entities.Spider;
import com.buglife.entities.Toy;
import com.buglife.entities.TripWire;
import com.buglife.levels.LevelConfig;
import com.buglife.levels.LevelConfigFactory;
import com.buglife.levels.FoodSpawnData;
import com.buglife.levels.SnailLocationData;
import com.buglife.levels.SpiderPatrolData;
import com.buglife.main.GameStateManager;
import com.buglife.world.World;

public class PlayingState extends GameState {
    private static final Logger logger = LoggerFactory.getLogger(PlayingState.class);

    // Level progression
    private String currentLevel = "level1";
    private final String[] levelOrder = { "level1", "level2" };
    private int currentLevelIndex = 0;
    
    // Current level configuration (loaded from levels/ package)
    private LevelConfig currentConfig;

    private Player player;
    private List<Spider> spiders;
    private Snail snail;
    private Toy toy;
    private List<Food> foods;
    private World world;
    private SoundManager soundManager;
    private List<TripWire> tripWires;

    private int cameraX, cameraY;
    private static final int VIRTUAL_WIDTH = 1366;
    private static final int VIRTUAL_HEIGHT = 768;

    private boolean snailHasTeleported = true;
    private int nextSnailLocationIndex = 1;
    private boolean playerHasInteractedWithSnail = false;

    private long levelStartTime;

    // private List<Point> foodSpawnPoints;

    private boolean hasBeenInitialized = false;

    private boolean isPaused = false;
    private int pauseMenuSelection = 0;
    private String[] pauseOptions = { "Resume", "Settings", "Restart", "Quit to Menu" };

    private static final Font HUD_FONT = new Font("Consolas", Font.PLAIN, 16);
    private static final Font MID_FONT = new Font("Consolas", Font.BOLD, 40);
    private static final Font BIG_FONT = new Font("Consolas", Font.BOLD, 80);

    public PlayingState(GameStateManager manager, SoundManager soundManager) {
        super(manager);
        this.soundManager = soundManager;
    }

    @Override
    public void init() {
        if (hasBeenInitialized) {
            isPaused = false;
            soundManager.stopAllSounds();
            soundManager.loopSound("music");
            return;
        }

        // Load level configuration from levels/ package
        currentConfig = LevelConfigFactory.getConfig(currentLevel);
        logger.info("Loading level: {} with config: {}", currentLevel, currentConfig.getClass().getSimpleName());

        // Initialize world
        world = new World(currentLevel);

        // Initialize player at level-specific spawn point
        Point playerSpawn = currentConfig.getPlayerSpawn();
        player = new Player(playerSpawn.x, playerSpawn.y, 32, 32);

        // Reset level timer
        levelStartTime = System.currentTimeMillis();

        // Initialize tripwires (if enabled)
        tripWires = new ArrayList<>();
        if (currentConfig.getMechanicsEnabled().isTripWiresEnabled()) {
            for (Point pos : currentConfig.getTripWirePositions()) {
                tripWires.add(new TripWire(pos.x, pos.y));
            }
        }

        // Initialize toy (if enabled)
        Point toySpawn = currentConfig.getToySpawn();
        if (currentConfig.getMechanicsEnabled().isToyEnabled() && toySpawn != null) {
            toy = new Toy();
            toy.setSpawnLocationPixels(toySpawn.x, toySpawn.y);
        } else {
            toy = null;
        }

        // Initialize spiders from config
        spiders = new ArrayList<>();
        for (SpiderPatrolData patrol : currentConfig.getSpiderPatrols()) {
            spiders.add(new Spider(patrol.getWaypoints()));
        }

        // Initialize snail from config
        List<Snail.SnailLocation> snailLocations = new ArrayList<>();
        for (SnailLocationData data : currentConfig.getSnailLocations()) {
            snailLocations.add(new Snail.SnailLocation(
                data.getPosition(),
                data.getDialogue(),
                data.isInteractionRequired()
            ));
        }
        snail = new Snail(player, snailLocations);
        snailHasTeleported = true;
        nextSnailLocationIndex = 1;
        playerHasInteractedWithSnail = false;

        // Initialize food from config
        foods = new ArrayList<>();
        boolean speedBoostEnabled = currentConfig.getMechanicsEnabled().isSpeedBoostFoodEnabled();
        for (FoodSpawnData foodData : currentConfig.getFoodSpawns()) {
            // Skip ENERGY_SEED if speed boost food is disabled
            if (foodData.isSpeedBoostFood() && !speedBoostEnabled) {
                continue;
            }
            Point tile = foodData.getTilePosition();
            int x = tile.x * World.TILE_SIZE + (World.TILE_SIZE / 4);
            int y = tile.y * World.TILE_SIZE + (World.TILE_SIZE / 4);
            foods.add(new Food(x, y, 20, foodData.getType()));
        }
        logger.debug("Food spawned: {} items for {}", foods.size(), currentLevel);

        soundManager.stopAllSounds();
        soundManager.loopSound("music");

        isPaused = false;

        hasBeenInitialized = true;
    }

    public void restart() {
        currentLevel = "level1";
        currentLevelIndex = 0;
        this.hasBeenInitialized = false;
        init();
    }

    public void setLevel(String levelName) {
        this.currentLevel = levelName;
        for (int i = 0; i < levelOrder.length; i++) {
            if (levelOrder[i].equals(levelName)) {
                currentLevelIndex = i;
                break;
            }
        }
        this.hasBeenInitialized = false;
        init();
    }

    public void goToNextLevel() {
        currentLevelIndex++;
        if (currentLevelIndex >= levelOrder.length) {
            // All levels complete - return to menu
            manager.setState(GameStateManager.MENU);
            return;
        }
        currentLevel = levelOrder[currentLevelIndex];
        this.hasBeenInitialized = false;
        init();
    }

    public String getCurrentLevel() {
        return currentLevel;
    }

    public boolean isLastLevel() {
        return currentLevelIndex >= levelOrder.length - 1;
    }

    @Override
    public void update() {
        if (isPaused) {
            return;
        }
        
        // Update PerformanceMonitor with current state info
        PerformanceMonitor monitor = PerformanceMonitor.getInstance();
        if (player != null) {
            monitor.setPlayerCoordinates(player.getCenterX(), player.getCenterY());
            boolean hasToy = toy != null && toy.isCarried();
            monitor.setPlayerState(
                player.getCurrentState(),
                player.getHunger(),
                player.getSpeed(),
                player.isWebbed(),
                hasToy
            );
        }
        monitor.setCurrentLevel(currentLevel);
        monitor.setEntityCounts(
            spiders != null ? spiders.size() : 0,
            snail != null ? snail.getLocationsCount() : 0,
            foods != null ? foods.size() : 0
        );

        if (snail != null && snail.getLocationsCount() > 1) {
            snail.update(world);
            boolean isSnailOnScreen = isRectOnScreen(snail.getX(), snail.getY(), snail.getWidth(),
                    snail.getHeight());

            if (isSnailOnScreen) {
                snailHasTeleported = false;
            }

            Snail.SnailLocation currentLocation = snail.getCurrentLocation();
            boolean interactionIsNeeded = currentLocation.requiresInteraction();

            boolean canTeleport = !isSnailOnScreen && !snailHasTeleported;
            if (interactionIsNeeded) {
                canTeleport = canTeleport && playerHasInteractedWithSnail;
            }

            if (canTeleport) {
                snail.teleportToLocation(nextSnailLocationIndex);
                nextSnailLocationIndex = (nextSnailLocationIndex + 1) % snail.getLocationsCount();
                snailHasTeleported = true;
                playerHasInteractedWithSnail = false;
            }
        }

        Iterator<TripWire> it = tripWires.iterator();
        while (it.hasNext()) {
            TripWire wire = it.next();

            if (wire.checkCollision(player)) {
                soundManager.playSound("webbed");

                Point noiseLocation = new Point(wire.getX() + 16, wire.getY() + 16);
                int radius = wire.getSoundRadius();

                for (Spider s : spiders) {
                    s.hearNoise(noiseLocation, radius);
                }

                it.remove();
            }
        }

        if (toy != null) {
            toy.update();
        }

        for (Spider spider : spiders) {
            if (spider != null) {
                spider.update(player, world, soundManager, toy);
            }
        }

        if (player != null) {
            player.update(world, soundManager);
        }
            
        if (player.hasDiedFromWeb()) {
            logger.info("Game Over: Player died from webbed state");
            soundManager.stopSound("music");
            soundManager.stopSound("chasing");
            soundManager.playSound("gameOver");
            manager.setState(GameStateManager.GAME_OVER);
            return;
        }

        handleSpiderAlerts();

        for (Spider currentSpider : spiders) {
            if (currentSpider != null) {
                double dx = player.getCenterX() - currentSpider.getCenterX();
                double dy = player.getCenterY() - currentSpider.getCenterY();
                double distance = Math.sqrt(dx * dx + dy * dy);
                double requiredDistance = player.getRadius() + currentSpider.getRadius();

                if (distance < requiredDistance) {
                    // Skip all damage if god mode is enabled
                    if (PerformanceMonitor.getInstance().isGodModeEnabled()) {
                        continue;
                    }
                    
                    if (player.getHunger() <= 0) {
                        logger.info("Game Over: Player caught with zero hunger");
                        soundManager.stopSound("music");
                        soundManager.stopSound("chasing");
                        soundManager.playSound("gameOver");
                        manager.setState(GameStateManager.GAME_OVER);
                        return;
                    }

                    if (currentSpider.isChasing()) {
                        if (player.isCrying()) {
                            logger.info("Game Over: Player caught by spider while crying");
                            soundManager.stopSound("music");
                            soundManager.stopSound("chasing");
                            soundManager.playSound("gameOver");
                            manager.setState(GameStateManager.GAME_OVER);
                            return;
                        } else if (!player.isWebImmune() && !player.isWebbed()) {
                            // Only web if not already webbed and not in the "struggle escape" window
                            player.getWebbed();
                            soundManager.playSound("webbed");
                        }
                    } else {
                        player.decreaseHunger(1);
                    }
                }
            }
        }

        cameraX = Math.max(0, Math.min(player.getCenterX() - (VIRTUAL_WIDTH / 2),
                world.getMapWidth() * World.TILE_SIZE - VIRTUAL_WIDTH));
        cameraY = Math.max(0, Math.min(player.getCenterY() - (VIRTUAL_HEIGHT / 2),
                world.getMapHeight() * World.TILE_SIZE - VIRTUAL_HEIGHT));

        for (int i = foods.size() - 1; i >= 0; i--) {
            Food currFood = foods.get(i);
            double dxFood = player.getCenterX() - currFood.getCenterX();
            double dyFood = player.getCenterY() - currFood.getCenterY();
            double distanceFood = Math.sqrt(dxFood * dxFood + dyFood * dyFood);
            double requiredDistanceFood = player.getRadius() + currFood.getRadius();

            if (distanceFood < requiredDistanceFood) {
                player.eat(currFood);
                soundManager.playSound("eat");
                foods.remove(i);
            }
        }

        if (player.isOnLevelCompleteTile()) {
            soundManager.stopAllSounds();
            soundManager.playSound("level_complete");
            manager.setState(GameStateManager.LEVEL_COMPLETE);
            return;
        }
        if (player.getHunger() <= 0 && !player.isCrying()) {
            soundManager.stopSound("music");
            soundManager.playSound("chasing");
            soundManager.playSound("gameOver");
            manager.setState(GameStateManager.GAME_OVER);
            return;
        }
    }

    @Override
    public void draw(Graphics2D g) {
        PerformanceMonitor monitor = PerformanceMonitor.getInstance();
        
        world.render(g, cameraX, cameraY, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);

        Graphics2D entityG2d = (Graphics2D) g.create();
        try {
            entityG2d.translate(-cameraX, -cameraY);
            
            // Draw tile grid overlay if enabled
            if (monitor.isShowTileGrid()) {
                drawTileGrid(entityG2d);
            }

            if (player != null) {
                player.render(entityG2d, world);
                
                // Draw player hitbox if enabled
                if (monitor.isShowHitboxes()) {
                    entityG2d.setColor(Color.GREEN);
                    entityG2d.setStroke(new BasicStroke(2));
                    // Player size is 32x32
                    int playerSize = 32;
                    entityG2d.drawRect(player.getCenterX() - playerSize/2, player.getCenterY() - playerSize/2, 
                        playerSize, playerSize);
                }
            }

            if (toy != null) {
                toy.draw(entityG2d);
                if (!toy.isCarried() && toy.canPickUp(player)) {
                    toy.drawInteractionPrompt(entityG2d);
                }
                
                // Draw toy hitbox if enabled
                if (monitor.isShowHitboxes() && !toy.isCarried()) {
                    entityG2d.setColor(Color.CYAN);
                    entityG2d.setStroke(new BasicStroke(2));
                    // Toy size is 24x24
                    int toySize = 24;
                    entityG2d.drawRect(toy.getCenterX() - toySize/2, toy.getCenterY() - toySize/2, 
                        toySize, toySize);
                }
            }

            for (TripWire wire : tripWires) {
                wire.draw(entityG2d);
                
                // Draw tripwire hitbox if enabled
                if (monitor.isShowHitboxes()) {
                    entityG2d.setColor(Color.MAGENTA);
                    entityG2d.setStroke(new BasicStroke(2));
                    entityG2d.drawRect(wire.getX(), wire.getY(), 32, 32);
                }
            }

            if (snail != null && snail.isVisible()) {
                snail.draw(entityG2d);
            }

            for (Spider spider : spiders) {
                if (spider != null) {
                    spider.draw(entityG2d);
                    
                    // Draw spider hitbox if enabled
                    if (monitor.isShowHitboxes()) {
                        entityG2d.setColor(Color.RED);
                        entityG2d.setStroke(new BasicStroke(2));
                        // Spider size is ~50x50
                        int spiderSize = 50;
                        entityG2d.drawRect(spider.getCenterX() - spiderSize/2, spider.getCenterY() - spiderSize/2, 
                            spiderSize, spiderSize);
                    }
                }
            }

            for (Food currFood : foods) {
                if (currFood != null) {
                    currFood.draw(entityG2d);
                    
                    // Draw food hitbox if enabled
                    if (monitor.isShowHitboxes()) {
                        entityG2d.setColor(Color.YELLOW);
                        entityG2d.setStroke(new BasicStroke(1));
                        // Food uses radius-based circular hitbox
                        double radius = currFood.getRadius();
                        int size = (int)(radius * 2);
                        entityG2d.drawOval(currFood.getCenterX() - (int)radius, currFood.getCenterY() - (int)radius, 
                            size, size);
                    }
                }
            }
        } finally {
            entityG2d.dispose();
        }

        drawHUD(g);

        if (isPaused) {
            drawPauseMenu(g);
        }
        
        // Draw level selection menu overlay (on top of everything, dev only)
        if (!PerformanceMonitor.isReleaseMode() && PerformanceMonitor.getInstance().isLevelMenuVisible()) {
            drawLevelSelectionMenu(g);
        }
    }

    private void drawHUD(Graphics2D g) {
        g.setColor(Color.DARK_GRAY);
        g.fillRect(10, 10, 200, 20);
        g.setColor(Color.ORANGE);
        if (player != null) {
            g.fillRect(10, 10, player.getHunger() * 2, 20);
        }
        g.setColor(Color.BLACK);
        g.drawRect(10, 10, 200, 20);

        if (player != null && player.getSpeedBoostTimer() > 0) {
            int maxBoostTime = 300; // 5 seconds * 60 frames
            int currentBoost = player.getSpeedBoostTimer();
            int barWidth = (int) ((double) currentBoost / maxBoostTime * 200);

            // Draw bar background
            g.setColor(Color.DARK_GRAY);
            g.fillRect(10, 35, 200, 10); // Positioned slightly below hunger bar

            // Draw green energy bar
            g.setColor(Color.GREEN);
            g.fillRect(10, 35, barWidth, 10);

            // Draw border
            g.setColor(Color.BLACK);
            g.drawRect(10, 35, 200, 10);

            // Optional Text
            g.setFont(new Font("Arial", Font.BOLD, 10));
            g.setColor(Color.WHITE);
            g.drawString("SPEED BOOST", 220, 44);
        }

        // Coordinates now shown in F3 debug overlay instead of here

        if (player != null && player.isWebbed()) {
            g.setColor(Color.WHITE);
            g.setFont(MID_FONT);
            String struggleMsg = "PRESS [SPACE] TO STRUGGLE!";
            int msgWidth = g.getFontMetrics().stringWidth(struggleMsg);
            g.drawString(struggleMsg, (VIRTUAL_WIDTH - msgWidth) / 2, VIRTUAL_HEIGHT - 100);
        }
    }
    
    /**
     * Draw tile grid overlay for debugging
     */
    private void drawTileGrid(Graphics2D g) {
        int tileSize = World.TILE_SIZE;
        int startCol = Math.max(0, cameraX / tileSize);
        int startRow = Math.max(0, cameraY / tileSize);
        int endCol = Math.min(world.getMapWidth(), (cameraX + VIRTUAL_WIDTH) / tileSize + 1);
        int endRow = Math.min(world.getMapHeight(), (cameraY + VIRTUAL_HEIGHT) / tileSize + 1);
        
        g.setFont(new Font("Monospaced", Font.PLAIN, 8));
        g.setStroke(new BasicStroke(1));
        
        for (int row = startRow; row < endRow; row++) {
            for (int col = startCol; col < endCol; col++) {
                int x = col * tileSize;
                int y = row * tileSize;
                
                // Draw grid lines
                boolean isSolid = world.isTileSolid(x, y);
                if (isSolid) {
                    g.setColor(new Color(255, 0, 0, 50)); // Red tint for solid tiles
                    g.fillRect(x, y, tileSize, tileSize);
                }
                
                g.setColor(new Color(100, 100, 100, 100)); // Gray grid lines
                g.drawRect(x, y, tileSize, tileSize);
                
                // Draw tile coordinates (every other tile to avoid clutter)
                if (row % 2 == 0 && col % 2 == 0) {
                    g.setColor(new Color(255, 255, 255, 150));
                    g.drawString(col + "," + row, x + 2, y + 10);
                }
            }
        }
    }

    private void drawPauseMenu(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);

        g.setColor(Color.WHITE);
        g.setFont(BIG_FONT);
        String msg = "PAUSED";
        int msgWidth = g.getFontMetrics().stringWidth(msg);
        g.drawString(msg, (VIRTUAL_WIDTH - msgWidth) / 2, VIRTUAL_HEIGHT / 3);

        g.setFont(MID_FONT);
        for (int i = 0; i < pauseOptions.length; i++) {
            if (i == pauseMenuSelection) {
                g.setColor(Color.YELLOW);
            } else {
                g.setColor(Color.WHITE);
            }
            int optionWidth = g.getFontMetrics().stringWidth(pauseOptions[i]);
            g.drawString(pauseOptions[i], (VIRTUAL_WIDTH - optionWidth) / 2,
                    VIRTUAL_HEIGHT / 2 + i * 60);
        }
    }
    
    private void drawLevelSelectionMenu(Graphics2D g) {
        // Semi-transparent overlay
        g.setColor(new Color(0, 0, 0, 200));
        g.fillRect(VIRTUAL_WIDTH / 2 - 200, VIRTUAL_HEIGHT / 2 - 200, 400, 400);
        
        // Border
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(3));
        g.drawRect(VIRTUAL_WIDTH / 2 - 200, VIRTUAL_HEIGHT / 2 - 200, 400, 400);
        
        // Title
        g.setFont(MID_FONT);
        String title = "Select Level";
        int titleWidth = g.getFontMetrics().stringWidth(title);
        g.drawString(title, VIRTUAL_WIDTH / 2 - titleWidth / 2, VIRTUAL_HEIGHT / 2 - 150);
        
        // Level options
        g.setFont(new Font("Consolas", Font.BOLD, 24));
        String[] levels = PerformanceMonitor.getInstance().getAvailableLevels();
        int selectedIndex = PerformanceMonitor.getInstance().getSelectedLevelIndex();
        
        for (int i = 0; i < levels.length; i++) {
            int y = VIRTUAL_HEIGHT / 2 - 80 + (i * 45);
            
            // Highlight selected level
            if (i == selectedIndex) {
                g.setColor(Color.YELLOW);
                g.fillRect(VIRTUAL_WIDTH / 2 - 180, y - 25, 360, 35);
                g.setColor(Color.BLACK);
            } else {
                g.setColor(Color.WHITE);
            }
            
            // Format level name nicely: "level1" -> "Level 1"
            String displayName = "Level " + levels[i].substring(5);
            int nameWidth = g.getFontMetrics().stringWidth(displayName);
            g.drawString(displayName, VIRTUAL_WIDTH / 2 - nameWidth / 2, y);
        }
        
        // Instructions
        g.setColor(Color.LIGHT_GRAY);
        g.setFont(new Font("Consolas", Font.PLAIN, 14));
        String instructions = "↑/↓ Navigate  |  ENTER Select  |  L/ESC Cancel";
        int instrWidth = g.getFontMetrics().stringWidth(instructions);
        g.drawString(instructions, VIRTUAL_WIDTH / 2 - instrWidth / 2, VIRTUAL_HEIGHT / 2 + 150);
    }

    @Override
    public void keyPressed(int keyCode) {
        if (isPaused) {
            handlePauseInput(keyCode);
            return;
        }

        if (keyCode == KeyEvent.VK_W || keyCode == KeyEvent.VK_UP) {
            player.movingUp = true;
        }
        if (keyCode == KeyEvent.VK_S || keyCode == KeyEvent.VK_DOWN) {
            player.movingDown = true;
        }
        if (keyCode == KeyEvent.VK_A || keyCode == KeyEvent.VK_LEFT) {
            player.movingLeft = true;
        }
        if (keyCode == KeyEvent.VK_D || keyCode == KeyEvent.VK_RIGHT) {
            player.movingRight = true;
        }

        // Dash ability (only if enabled for this level)
        if (keyCode == KeyEvent.VK_SHIFT && currentConfig.getMechanicsEnabled().isDashEnabled()) {
            int dirX = 0, dirY = 0;
            if (player.movingUp)
                dirY = -1;
            if (player.movingDown)
                dirY = 1;
            if (player.movingLeft)
                dirX = -1;
            if (player.movingRight)
                dirX = 1;
            player.dash(dirX, dirY, soundManager);
        }

        if (keyCode == KeyEvent.VK_SPACE) {
            player.struggle();
            soundManager.playSound("struggle");
        }

        if (keyCode == KeyEvent.VK_ESCAPE) {
            isPaused = true;
            pauseMenuSelection = 0;
            soundManager.stopSound("music");
            soundManager.stopSound("chasing");
        }

        if (keyCode == KeyEvent.VK_E) {
            if (snail != null && snail.canInteract(player)) {
                snail.interact();
                playerHasInteractedWithSnail = true;
            } else if (toy != null && currentConfig.getMechanicsEnabled().isToyEnabled() && toy.canPickUp(player)) {
                toy.pickUp(player);
            }
        }

        // Toy throw (only if toy mechanic is enabled)
        if (keyCode == KeyEvent.VK_F && currentConfig.getMechanicsEnabled().isToyEnabled()) {
            if (toy != null && toy.isCarried()) {
                toy.throwToy(player.getCenterX(), player.getCenterY(), player.getFacingDirection());
                soundManager.playSound("throw");
            }
        }
        
        // L key: Toggle level selection menu (dev only)
        if (keyCode == KeyEvent.VK_L && !PerformanceMonitor.isReleaseMode()) {
            PerformanceMonitor.getInstance().toggleLevelMenu();
            return;
        }
        
        // Handle level menu navigation when visible
        if (!PerformanceMonitor.isReleaseMode() && PerformanceMonitor.getInstance().isLevelMenuVisible()) {
            if (keyCode == KeyEvent.VK_UP || keyCode == KeyEvent.VK_W) {
                PerformanceMonitor.getInstance().levelSelectionUp();
                soundManager.playSound("menu");
                return;
            }
            if (keyCode == KeyEvent.VK_DOWN || keyCode == KeyEvent.VK_S) {
                PerformanceMonitor.getInstance().levelSelectionDown();
                soundManager.playSound("menu");
                return;
            }
            if (keyCode == KeyEvent.VK_ENTER) {
                String selectedLevel = PerformanceMonitor.getInstance().getSelectedLevel();
                setLevel(selectedLevel);
                PerformanceMonitor.getInstance().toggleLevelMenu();
                logger.info("Switched to level: {}", selectedLevel);
                return;
            }
            // ESC closes the level menu without selecting
            // (will be handled by pause menu check above)
        }
        
        // F12: Export game state for debugging (dev only)
        if (keyCode == KeyEvent.VK_F12 && !PerformanceMonitor.isReleaseMode()) {
            try {
                PerformanceMonitor monitor = PerformanceMonitor.getInstance();
                Class<?> exporterClass = Class.forName("com.buglife.utils.DebugExporter");
                java.lang.reflect.Method exportMethod = exporterClass.getMethod(
                    "exportGameState", String.class, int.class, int.class, int.class,
                    String.class, int.class, int.class, int.class);
                exportMethod.invoke(null,
                    currentLevel,
                    player.getCenterX(),
                    player.getCenterY(),
                    player.getHunger(),
                    player.getCurrentState(),
                    spiders.size(),
                    snail != null ? snail.getLocationsCount() : 0,
                    foods.size());
                logger.info("Game state exported via F12");
            } catch (Exception e) {
                logger.warn("Debug export not available");
            }
        }
    }

    @Override
    public void keyReleased(int keyCode) {
        if (keyCode == KeyEvent.VK_W || keyCode == KeyEvent.VK_UP) {
            player.movingUp = false;
        }
        if (keyCode == KeyEvent.VK_S || keyCode == KeyEvent.VK_DOWN) {
            player.movingDown = false;
        }
        if (keyCode == KeyEvent.VK_A || keyCode == KeyEvent.VK_LEFT) {
            player.movingLeft = false;
        }
        if (keyCode == KeyEvent.VK_D || keyCode == KeyEvent.VK_RIGHT) {
            player.movingRight = false;
        }
    }

    private void handlePauseInput(int keyCode) {
        if (keyCode == KeyEvent.VK_UP || keyCode == KeyEvent.VK_W) {
            pauseMenuSelection--;
            if (pauseMenuSelection < 0) {
                pauseMenuSelection = pauseOptions.length - 1;
            }
            soundManager.playSound("menu");
        }

        if (keyCode == KeyEvent.VK_DOWN || keyCode == KeyEvent.VK_S) {
            pauseMenuSelection++;
            if (pauseMenuSelection >= pauseOptions.length) {
                pauseMenuSelection = 0;
            }
            soundManager.playSound("menu");
        }

        if (keyCode == KeyEvent.VK_ESCAPE) {
            isPaused = false;
            soundManager.loopSound("music");
        }

        if (keyCode == KeyEvent.VK_ENTER) {
            if (pauseOptions[pauseMenuSelection].equals("Resume")) {
                isPaused = false;
                soundManager.loopSound("music");
            } else if (pauseOptions[pauseMenuSelection].equals("Settings")) {
                manager.getSettingsState().setReturnState(GameStateManager.PLAYING);
                manager.setState(GameStateManager.SETTINGS);
            } else if (pauseOptions[pauseMenuSelection].equals("Restart")) {
                init();
            } else if (pauseOptions[pauseMenuSelection].equals("Quit to Menu")) {
                // === THE RAGE QUIT SAVE ===
                // Emergency save of exact coordinates before quitting
                saveCurrentState();
                logger.info("Rage quit save completed");
                manager.setState(GameStateManager.MENU);
            }
        }
    }

    @Override
    public void cleanup() {
        soundManager.stopAllSounds();
    }

    private boolean isRectOnScreen(int x, int y, int width, int height) {
        return (x < cameraX + VIRTUAL_WIDTH &&
                x + width > cameraX &&
                y < cameraY + VIRTUAL_HEIGHT &&
                y + height > cameraY);
    }

    // ============================================================
    // OLD LEVEL-SPECIFIC METHODS (Removed - now in levels/ package)
    // 
    // Spider patrols   → See Level1Config.java, Level2Config.java, etc.
    // Snail locations  → See Level1Config.java, Level2Config.java, etc.
    // Food spawns      → See Level1Config.java, Level2Config.java, etc.
    // TripWires        → See LevelTestConfig.java
    // Toy spawn        → See LevelTestConfig.java
    // ============================================================

    public boolean isInitialized() {
        return hasBeenInitialized;
    }

    /**
     * Get the current level index for save data.
     */
    public int getCurrentLevelIndex() {
        return currentLevelIndex;
    }

    /**
     * Get the remaining foods list for save data.
     */
    public List<Food> getFoods() {
        return foods;
    }

    /**
     * Get the toy entity for save data.
     */
    public Toy getToy() {
        return toy;
    }

    /**
     * Get the player entity for save data.
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Save the current game state — The Checkpoint Ritual.
     * Called on: level complete auto-save, rage quit, manual save.
     * 
     * @return true if save succeeded
     */
    public boolean saveCurrentState() {
        if (player == null || !hasBeenInitialized) {
            logger.warn("Cannot save: game not initialized");
            return false;
        }
        return SaveManager.saveGame(player, currentLevel, currentLevelIndex, foods, toy);
    }

    /**
     * Load a game from a SaveData snapshot — Resurrect a frozen moment.
     * Restores player position, hunger, inventory, and world state.
     */
    public void loadFromSave(SaveData saveData) {
        if (saveData == null) {
            logger.error("Cannot load: SaveData is null");
            return;
        }

        // Set level from save
        this.currentLevel = saveData.getLevelId();
        this.currentLevelIndex = saveData.getLevelIndex();
        this.hasBeenInitialized = false;

        // Initialize the level normally first
        init();

        // Now override with saved state
        if (player != null) {
            // Restore position
            player.setPosition(saveData.getPlayerX(), saveData.getPlayerY());

            // Restore hunger
            player.setHunger(saveData.getHunger());

            // Restore speed boost
            player.setSpeedBoostTimer(saveData.getSpeedBoostTimer());
        }

        // Restore food state (remove foods that were already eaten)
        if (saveData.getRemainingFoods() != null && foods != null) {
            List<SaveData.FoodState> savedFoods = saveData.getRemainingFoods();
            // Only keep foods that exist in the save
            foods.removeIf(food -> {
                boolean found = false;
                for (SaveData.FoodState sf : savedFoods) {
                    if (Math.abs(food.getCenterX() - sf.getX()) < 5 &&
                        Math.abs(food.getCenterY() - sf.getY()) < 5) {
                        found = true;
                        break;
                    }
                }
                return !found;
            });
        }

        logger.info("Game loaded from save: Level={}, Pos=({},{}), Hunger={}",
                saveData.getLevelId(), (int)saveData.getPlayerX(),
                (int)saveData.getPlayerY(), saveData.getHunger());
    }

    public void pauseGame() {
        isPaused = true;
    }

    public void resumeGame() {
        isPaused = false;
        soundManager.loopSound("music");
    }

    private void handleSpiderAlerts() {
        if (player.isCrying()) {
            for (Spider spider : spiders) {
                if (spider != null && spider.getCurrentState() == Spider.SpiderState.PATROLLING) {
                    spider.startChasing(player, soundManager);
                }
            }
        }
    }
}