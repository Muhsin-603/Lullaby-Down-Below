package src.com.buglife.states;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;

import src.com.buglife.assets.SoundManager;
import src.com.buglife.entities.Food;
import src.com.buglife.entities.Player;
import src.com.buglife.entities.Snail;
import src.com.buglife.entities.Spider;
import src.com.buglife.entities.Toy;
import src.com.buglife.entities.TripWire;
import src.com.buglife.main.GameStateManager;
import src.com.buglife.world.World;

public class PlayingState extends GameState {
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

    // private List<Point> foodSpawnPoints;

    private boolean hasBeenInitialized = false;

    private boolean isPaused = false;
    private int pauseMenuSelection = 0;
    private String[] pauseOptions = { "Resume", "Restart", "Quit to Menu" };

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

        tripWires = new ArrayList<>();
        initTripWires();
        world = new World();

        player = new Player(594, 2484, 32, 32);

        toy = new Toy();
        toy.setSpawnLocationPixels(574, 2256);

        spiders = new ArrayList<>();
        initializeSpiders();

        snail = new Snail(player, initializeSnailLocations());
        snailHasTeleported = true;
        nextSnailLocationIndex = 1;
        playerHasInteractedWithSnail = false;

        foods = new ArrayList<>();
        spawnFood();

        soundManager.stopAllSounds();
        soundManager.loopSound("music");

        isPaused = false;

        hasBeenInitialized = true;
    }

    private void initTripWires() {
        tripWires.add(new TripWire(1718, 1770));
        tripWires.add(new TripWire(800, 2400));
    }

    public void restart() {
        this.hasBeenInitialized = false;
        init();
    }

    @Override
    public void update() {
        if (isPaused) {
            return;
        }

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
            System.out.println("GAME OVER: Died By Webbed State");
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
                    if (player.getHunger() <= 0) {
                        System.out.println("GAME OVER: Player caught with zero hunger!");
                        soundManager.stopSound("music");
                        soundManager.stopSound("chasing");
                        soundManager.playSound("gameOver");
                        manager.setState(GameStateManager.GAME_OVER);
                        return;
                    }

                    if (currentSpider.isChasing()) {
                        if (player.isCrying()) {
                            soundManager.stopSound("music");
                            soundManager.stopSound("chasing");
                            soundManager.playSound("gameOver");
                            manager.setState(GameStateManager.GAME_OVER);
                            return;
                        } else {
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
        world.render(g, cameraX, cameraY, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);

        Graphics2D entityG2d = (Graphics2D) g.create();
        try {
            entityG2d.translate(-cameraX, -cameraY);

            if (player != null) {
                player.render(entityG2d, world);
            }

            if (toy != null) {
                toy.draw(entityG2d);
                if (!toy.isCarried() && toy.canPickUp(player)) {
                    toy.drawInteractionPrompt(entityG2d);
                }
            }

            for (TripWire wire : tripWires) {
                wire.draw(entityG2d);
            }

            if (snail != null && snail.isVisible()) {
                snail.draw(entityG2d);
            }

            for (Spider spider : spiders) {
                if (spider != null) {
                    spider.draw(entityG2d);
                }
            }

            for (Food currFood : foods) {
                if (currFood != null) {
                    currFood.draw(entityG2d);
                }
            }
        } finally {
            entityG2d.dispose();
        }

        drawHUD(g);

        if (isPaused) {
            drawPauseMenu(g);
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

        if (player != null) {
            g.setFont(HUD_FONT);
            g.setColor(Color.WHITE);
            String coords = "X: " + player.getCenterX() + " | Y: " + player.getCenterY();
            int coordsWidth = g.getFontMetrics().stringWidth(coords);
            g.drawString(coords, VIRTUAL_WIDTH - coordsWidth - 15, 25);
        }

        if (player != null && player.isWebbed()) {
            g.setColor(Color.WHITE);
            g.setFont(MID_FONT);
            String struggleMsg = "PRESS [SPACE] TO STRUGGLE!";
            int msgWidth = g.getFontMetrics().stringWidth(struggleMsg);
            g.drawString(struggleMsg, (VIRTUAL_WIDTH - msgWidth) / 2, VIRTUAL_HEIGHT - 100);
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

        if (keyCode == KeyEvent.VK_SHIFT) {
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
            } else if (toy != null && toy.canPickUp(player)) {
                toy.pickUp(player);
            }
        }

        if (keyCode == KeyEvent.VK_F) {
            if (toy != null && toy.isCarried()) {
                toy.throwToy(player.getCenterX(), player.getCenterY(), player.getFacingDirection());
                soundManager.playSound("throw");
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
            } else if (pauseOptions[pauseMenuSelection].equals("Restart")) {
                init();
            } else if (pauseOptions[pauseMenuSelection].equals("Quit to Menu")) {
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

    private void initializeSpiders() {
        List<Point> patrolPath1 = new ArrayList<>();
        patrolPath1.add(new Point(23, 23));
        patrolPath1.add(new Point(29, 23));
        patrolPath1.add(new Point(29, 25));
        patrolPath1.add(new Point(23, 25));
        patrolPath1.add(new Point(23, 23));

        List<Point> patrolPath2 = new ArrayList<>();
        patrolPath2.add(new Point(2, 2));
        patrolPath2.add(new Point(8, 2));
        patrolPath2.add(new Point(8, 7));
        patrolPath2.add(new Point(2, 7));
        patrolPath2.add(new Point(2, 2));

        List<Point> patrolPath3 = new ArrayList<>();
        patrolPath3.add(new Point(26, 1));
        patrolPath3.add(new Point(26, 10));
        patrolPath3.add(new Point(26, 1));

        List<Point> patrolPath4 = new ArrayList<>();
        patrolPath4.add(new Point(7, 10));
        patrolPath4.add(new Point(15, 10));
        patrolPath4.add(new Point(15, 15));
        patrolPath4.add(new Point(7, 15));
        patrolPath4.add(new Point(7, 10));

        spiders.add(new Spider(patrolPath1));
        spiders.add(new Spider(patrolPath2));
        spiders.add(new Spider(patrolPath3));
        spiders.add(new Spider(patrolPath4));
    }

    private List<Snail.SnailLocation> initializeSnailLocations() {
        List<Snail.SnailLocation> locations = new ArrayList<>();
        locations.add(new Snail.SnailLocation(
                new Point(534, 2464),
                new String[] { "Hello little one...", "Be careful of the spiders!" },
                true));
        locations.add(new Snail.SnailLocation(
                new Point(938, 1754),
                new String[] { "You shouldn't stay hungry", "Eat these berries.", "These give you energy" },
                true));
        locations.add(new Snail.SnailLocation(
                new Point(1116, 976),
                new String[] { "There are dark shadows.", "You can hide from the spiders in it" },
                true));
        locations.add(new Snail.SnailLocation(
                new Point(2166, 136),
                new String[] { "Stay safe", "Climb these ladders to the next floor.", "Farewell little one...!" },
                true));
        return locations;
    }

    private void spawnFoodAtTile(int tileX, int tileY, Food.FoodType type) {
        int x = tileX * World.TILE_SIZE + (World.TILE_SIZE / 4);
        int y = tileY * World.TILE_SIZE + (World.TILE_SIZE / 4);
        foods.add(new Food(x, y, 20, type));
    }

    // 2. Replace the old spawnFood() method with this "Manual Control" version
    private void spawnFood() {
        foods = new ArrayList<>();
        
        // --- LEVEL DESIGNER AREA ---
        // Place your food here! 
        // (TileX, TileY, Type)
        
        // The easy snacks
        spawnFoodAtTile(16, 27, Food.FoodType.BERRY);
        spawnFoodAtTile(34, 25, Food.FoodType.BERRY);
        
        // The strategic boosts (Green!)
        spawnFoodAtTile(22, 10, Food.FoodType.ENERGY_SEED); 
        spawnFoodAtTile(15, 15, Food.FoodType.ENERGY_SEED); 
        
        // More berries...
        spawnFoodAtTile(5, 5, Food.FoodType.BERRY);
        
        System.out.println("Food spawned: " + foods.size());
    }

    private void handleSpiderAlerts() {
        if (player.isCrying()) {
            for (Spider spider : spiders) {
                if (spider != null && spider.getCurrentState() == Spider.SpiderState.PATROLLING) {
                    spider.setReturnPoint(new Point(spider.getCenterX(), spider.getCenterY()));
                    spider.startChasing(player, soundManager);
                }
            }
        }
    }
}