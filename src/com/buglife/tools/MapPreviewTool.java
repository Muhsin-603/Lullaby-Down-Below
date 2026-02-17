package com.buglife.tools;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;

import com.buglife.config.TileConstants;
import com.buglife.entities.Food;
import com.buglife.levels.*;

/**
 * Standalone Map Preview Tool for Level Designers.
 * 
 * Run this separately from the main game to:
 * - Preview maps at low resolution
 * - See snail/spider/food markers
 * - Live-reload on file changes
 * - Pan/zoom the map
 * - Export preview images
 * 
 * Usage: Run main() method directly
 */
public class MapPreviewTool extends JFrame {
    
    // ========== CONSTANTS ==========
    private static final int GAME_TILE_SIZE = 64;  // Actual game tile size
    private static final int DEFAULT_PREVIEW_TILE_SIZE = 16;  // Default preview tile size
    private static final int MIN_TILE_SIZE = 4;
    private static final int MAX_TILE_SIZE = 64;
    
    // ========== STATE ==========
    private String currentLevel = "level1";
    private int[][] mapData;
    private int mapWidth;
    private int mapHeight;
    private LevelConfig levelConfig;
    
    // Preview settings
    private int previewTileSize = DEFAULT_PREVIEW_TILE_SIZE;
    private boolean showGrid = true;
    private boolean showSnails = true;
    private boolean showSpiders = true;
    private boolean showFood = true;
    private boolean showTripwires = true;
    private boolean showPlayerSpawn = true;
    private boolean showToySpawn = true;
    private boolean showCoordinates = true;
    private boolean liveReload = true;
    
    // Pan/zoom
    private int panX = 0;
    private int panY = 0;
    private Point lastMousePos;
    private boolean isPanning = false;
    
    // Measurement mode
    private boolean measureMode = false;
    private Point measureStart = null;
    private Point measureEnd = null;
    
    // UI Components
    private MapCanvas canvas;
    private JLabel statusLabel;
    private JLabel coordLabel;
    private JComboBox<String> levelSelector;
    private FileWatcher fileWatcher;
    private JTextArea entityInfoArea;
    private JTabbedPane rightTabs;
    
    // ========== TILE COLORS (for symbolic rendering) ==========
    private static final Map<Integer, Color> TILE_COLORS = new HashMap<>();
    static {
        TILE_COLORS.put(TileConstants.FLOOR, new Color(60, 50, 40));
        TILE_COLORS.put(TileConstants.WALL, new Color(80, 70, 60));
        TILE_COLORS.put(TileConstants.WALL_ALT, new Color(90, 75, 65));
        TILE_COLORS.put(TileConstants.STICKY_FLOOR, new Color(100, 80, 50));
        TILE_COLORS.put(TileConstants.BROKEN_TILE, new Color(70, 60, 55));
        TILE_COLORS.put(TileConstants.SHADOW_TILE, new Color(30, 25, 20));
        TILE_COLORS.put(TileConstants.STAIN_1, new Color(55, 45, 35));
        TILE_COLORS.put(TileConstants.STAIN_2, new Color(55, 45, 35));
        TILE_COLORS.put(TileConstants.STAIN_3, new Color(55, 45, 35));
        TILE_COLORS.put(TileConstants.STAIN_4, new Color(55, 45, 35));
        TILE_COLORS.put(TileConstants.SACK_W1, new Color(139, 90, 43));
        TILE_COLORS.put(TileConstants.SACK_W2, new Color(139, 90, 43));
        TILE_COLORS.put(TileConstants.SACK_W3, new Color(139, 90, 43));
        TILE_COLORS.put(TileConstants.SACK_W4, new Color(139, 90, 43));
        TILE_COLORS.put(TileConstants.PLANK_1, new Color(160, 120, 80));
        TILE_COLORS.put(TileConstants.PLANK_2, new Color(160, 120, 80));
        TILE_COLORS.put(TileConstants.PLANK_3, new Color(160, 120, 80));
        TILE_COLORS.put(TileConstants.PLANK_4, new Color(160, 120, 80));
        TILE_COLORS.put(TileConstants.LADDER_1, new Color(120, 80, 40));
        TILE_COLORS.put(TileConstants.LADDER_2, new Color(120, 80, 40));
        TILE_COLORS.put(TileConstants.LADDER_3, new Color(0, 200, 100));  // Level complete - bright green
        TILE_COLORS.put(TileConstants.LADDER_4, new Color(120, 80, 40));
        TILE_COLORS.put(TileConstants.INTRO_TILE_1, new Color(50, 50, 60));
        TILE_COLORS.put(TileConstants.INTRO_TILE_2, new Color(50, 50, 60));
        TILE_COLORS.put(TileConstants.INTRO_TILE_3, new Color(50, 50, 60));
        TILE_COLORS.put(TileConstants.INTRO_TILE_4, new Color(70, 70, 80));
        TILE_COLORS.put(TileConstants.INTRO_TILE_5, new Color(70, 70, 80));
        TILE_COLORS.put(TileConstants.INTRO_TILE_6, new Color(70, 70, 80));
    }
    
    // ========== ENTRY POINT ==========
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                // Use default look and feel
            }
            new MapPreviewTool().setVisible(true);
        });
    }
    
    // ========== CONSTRUCTOR ==========
    public MapPreviewTool() {
        super("Map Preview Tool - Lullaby Down Below");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);
        
        initUI();
        loadLevel(currentLevel);
        startFileWatcher();
    }
    
    private void initUI() {
        setLayout(new BorderLayout());
        
        // === TOP TOOLBAR ===
        JPanel toolbar = createToolbar();
        add(toolbar, BorderLayout.NORTH);
        
        // === MAIN CANVAS ===
        canvas = new MapCanvas();
        JScrollPane scrollPane = new JScrollPane(canvas);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        add(scrollPane, BorderLayout.CENTER);
        
        // === RIGHT PANEL (Legend + Options) ===
        JPanel rightPanel = createRightPanel();
        add(rightPanel, BorderLayout.EAST);
        
        // === BOTTOM STATUS BAR ===
        JPanel statusBar = createStatusBar();
        add(statusBar, BorderLayout.SOUTH);
        
        // Setup keyboard shortcuts
        setupKeyBindings();
    }
    
    private JPanel createToolbar() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        toolbar.setBorder(new EmptyBorder(5, 10, 5, 10));
        
        // Level selector
        toolbar.add(new JLabel("Level:"));
        String[] levels = {"level1", "level2", "level3", "level4", "level5", "level_test"};
        levelSelector = new JComboBox<>(levels);
        levelSelector.addActionListener(e -> loadLevel((String) levelSelector.getSelectedItem()));
        toolbar.add(levelSelector);
        
        toolbar.add(Box.createHorizontalStrut(20));
        
        // Zoom controls
        toolbar.add(new JLabel("Zoom:"));
        JSlider zoomSlider = new JSlider(MIN_TILE_SIZE, MAX_TILE_SIZE, previewTileSize);
        zoomSlider.setPreferredSize(new Dimension(150, 25));
        zoomSlider.addChangeListener(e -> {
            previewTileSize = zoomSlider.getValue();
            canvas.repaint();
        });
        toolbar.add(zoomSlider);
        
        toolbar.add(Box.createHorizontalStrut(20));
        
        // Action buttons
        JButton reloadBtn = new JButton("⟳ Reload");
        reloadBtn.addActionListener(e -> reloadCurrentLevel());
        toolbar.add(reloadBtn);
        
        JButton fitBtn = new JButton("⊡ Fit View");
        fitBtn.addActionListener(e -> fitToView());
        toolbar.add(fitBtn);
        
        JButton exportBtn = new JButton("📷 Export PNG");
        exportBtn.addActionListener(e -> exportToPNG());
        toolbar.add(exportBtn);
        
        JButton resetPanBtn = new JButton("⌂ Reset Pan");
        resetPanBtn.addActionListener(e -> {
            panX = 0;
            panY = 0;
            canvas.repaint();
        });
        toolbar.add(resetPanBtn);
        
        toolbar.add(Box.createHorizontalStrut(10));
        
        // Measurement tool
        JToggleButton measureBtn = new JToggleButton("📏 Measure");
        measureBtn.addActionListener(e -> {
            measureMode = measureBtn.isSelected();
            measureStart = null;
            measureEnd = null;
            canvas.setCursor(measureMode ? Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR) : Cursor.getDefaultCursor());
            canvas.repaint();
            if (measureMode) {
                updateStatus("Measure mode: Click to set start point, click again for end point");
            } else {
                updateStatus("Ready");
            }
        });
        toolbar.add(measureBtn);
        
        return toolbar;
    }
    
    private JPanel createRightPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(5, 5, 5, 5));
        panel.setPreferredSize(new Dimension(260, 0));
        
        rightTabs = new JTabbedPane();
        
        // === TAB 1: VISIBILITY OPTIONS ===
        JPanel optionsTab = createOptionsTab();
        rightTabs.addTab("Options", optionsTab);
        
        // === TAB 2: TILE PALETTE ===
        JPanel paletteTab = createTilePaletteTab();
        rightTabs.addTab("Tiles", paletteTab);
        
        // === TAB 3: ENTITY INSPECTOR ===
        JPanel inspectorTab = createEntityInspectorTab();
        rightTabs.addTab("Entities", inspectorTab);
        
        // === TAB 4: STATISTICS ===
        JPanel statsTab = createStatsTab();
        rightTabs.addTab("Stats", statsTab);
        
        panel.add(rightTabs, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createOptionsTab() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(5, 5, 5, 5));
        
        // === VISIBILITY OPTIONS ===
        JPanel optionsPanel = new JPanel();
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));
        optionsPanel.setBorder(BorderFactory.createTitledBorder("Show/Hide"));
        
        JCheckBox gridCb = new JCheckBox("Grid Lines", showGrid);
        gridCb.addActionListener(e -> { showGrid = gridCb.isSelected(); canvas.repaint(); });
        optionsPanel.add(gridCb);
        
        JCheckBox snailCb = new JCheckBox("Snails 🐌", showSnails);
        snailCb.addActionListener(e -> { showSnails = snailCb.isSelected(); canvas.repaint(); });
        optionsPanel.add(snailCb);
        
        JCheckBox spiderCb = new JCheckBox("Spiders 🕷", showSpiders);
        spiderCb.addActionListener(e -> { showSpiders = spiderCb.isSelected(); canvas.repaint(); });
        optionsPanel.add(spiderCb);
        
        JCheckBox foodCb = new JCheckBox("Food 🍎", showFood);
        foodCb.addActionListener(e -> { showFood = foodCb.isSelected(); canvas.repaint(); });
        optionsPanel.add(foodCb);
        
        JCheckBox tripCb = new JCheckBox("Tripwires ⚡", showTripwires);
        tripCb.addActionListener(e -> { showTripwires = tripCb.isSelected(); canvas.repaint(); });
        optionsPanel.add(tripCb);
        
        JCheckBox playerCb = new JCheckBox("Player Spawn 🎮", showPlayerSpawn);
        playerCb.addActionListener(e -> { showPlayerSpawn = playerCb.isSelected(); canvas.repaint(); });
        optionsPanel.add(playerCb);
        
        JCheckBox toyCb = new JCheckBox("Toy Spawn 🧸", showToySpawn);
        toyCb.addActionListener(e -> { showToySpawn = toyCb.isSelected(); canvas.repaint(); });
        optionsPanel.add(toyCb);
        
        JCheckBox coordCb = new JCheckBox("Coordinates", showCoordinates);
        coordCb.addActionListener(e -> { showCoordinates = coordCb.isSelected(); canvas.repaint(); });
        optionsPanel.add(coordCb);
        
        panel.add(optionsPanel);
        panel.add(Box.createVerticalStrut(10));
        
        // === LIVE RELOAD ===
        JPanel reloadPanel = new JPanel();
        reloadPanel.setLayout(new BoxLayout(reloadPanel, BoxLayout.Y_AXIS));
        reloadPanel.setBorder(BorderFactory.createTitledBorder("Live Reload"));
        
        JCheckBox liveCb = new JCheckBox("Watch for file changes", liveReload);
        liveCb.addActionListener(e -> {
            liveReload = liveCb.isSelected();
            if (liveReload) startFileWatcher();
            else stopFileWatcher();
        });
        reloadPanel.add(liveCb);
        
        panel.add(reloadPanel);
        panel.add(Box.createVerticalStrut(10));
        
        // === LEGEND ===
        JPanel legendPanel = new JPanel();
        legendPanel.setLayout(new BoxLayout(legendPanel, BoxLayout.Y_AXIS));
        legendPanel.setBorder(BorderFactory.createTitledBorder("Legend"));
        
        addLegendItem(legendPanel, Color.CYAN, "🐌 Snail Location");
        addLegendItem(legendPanel, Color.RED, "🕷 Spider Patrol");
        addLegendItem(legendPanel, Color.YELLOW, "🍎 Berry");
        addLegendItem(legendPanel, Color.GREEN, "⚡ Energy Seed");
        addLegendItem(legendPanel, Color.ORANGE, "⚡ Tripwire");
        addLegendItem(legendPanel, Color.BLUE, "🎮 Player Spawn");
        addLegendItem(legendPanel, Color.MAGENTA, "🧸 Toy Spawn");
        addLegendItem(legendPanel, new Color(0, 200, 100), "🚪 Exit (Ladder)");
        
        panel.add(legendPanel);
        panel.add(Box.createVerticalGlue());
        
        // === INFO PANEL ===
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBorder(BorderFactory.createTitledBorder("Controls"));
        
        JLabel infoLabel = new JLabel("<html>Mouse wheel: Zoom<br>Right-drag: Pan<br>R: Reload | F: Fit<br>Arrow keys: Pan</html>");
        infoLabel.setFont(infoLabel.getFont().deriveFont(11f));
        infoPanel.add(infoLabel);
        
        panel.add(infoPanel);
        
        return panel;
    }
    
    private JPanel createTilePaletteTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(5, 5, 5, 5));
        
        // Create tile palette grid
        JPanel paletteGrid = new JPanel(new GridLayout(0, 4, 2, 2));
        
        // Add all known tile types
        Object[][] tileInfo = {
            {TileConstants.FLOOR, "Floor", false},
            {TileConstants.WALL, "Wall", true},
            {TileConstants.WALL_ALT, "Wall Alt", true},
            {TileConstants.STICKY_FLOOR, "Sticky", false},
            {TileConstants.BROKEN_TILE, "Broken", true},
            {TileConstants.SHADOW_TILE, "Shadow", false},
            {TileConstants.STAIN_1, "Stain 1", false},
            {TileConstants.STAIN_2, "Stain 2", false},
            {TileConstants.STAIN_3, "Stain 3", false},
            {TileConstants.STAIN_4, "Stain 4", false},
            {TileConstants.SACK_W1, "Sack 1", true},
            {TileConstants.SACK_W2, "Sack 2", true},
            {TileConstants.SACK_W3, "Sack 3", false},
            {TileConstants.SACK_W4, "Sack 4", true},
            {TileConstants.PLANK_1, "Plank 1", false},
            {TileConstants.PLANK_2, "Plank 2", false},
            {TileConstants.PLANK_3, "Plank 3", false},
            {TileConstants.PLANK_4, "Plank 4", false},
            {TileConstants.LADDER_1, "Ladder 1", true},
            {TileConstants.LADDER_2, "Ladder 2", true},
            {TileConstants.LADDER_3, "Exit", false},
            {TileConstants.LADDER_4, "Ladder 4", true},
            {TileConstants.INTRO_TILE_1, "Intro 1", false},
            {TileConstants.INTRO_TILE_2, "Intro 2", false},
            {TileConstants.INTRO_TILE_3, "Intro 3", false},
            {TileConstants.INTRO_TILE_4, "Intro 4", true},
            {TileConstants.INTRO_TILE_5, "Intro 5", true},
            {TileConstants.INTRO_TILE_6, "Intro 6", true},
        };
        
        for (Object[] info : tileInfo) {
            int id = (Integer) info[0];
            String name = (String) info[1];
            boolean solid = (Boolean) info[2];
            
            JPanel tilePanel = new JPanel(new BorderLayout());
            tilePanel.setBorder(BorderFactory.createLineBorder(solid ? Color.RED : Color.GRAY));
            tilePanel.setToolTipText("ID: " + id + " - " + name + (solid ? " (SOLID)" : ""));
            
            JPanel colorBox = new JPanel();
            colorBox.setBackground(TILE_COLORS.getOrDefault(id, Color.GRAY));
            colorBox.setPreferredSize(new Dimension(30, 30));
            tilePanel.add(colorBox, BorderLayout.CENTER);
            
            JLabel idLabel = new JLabel(String.valueOf(id), SwingConstants.CENTER);
            idLabel.setFont(idLabel.getFont().deriveFont(9f));
            tilePanel.add(idLabel, BorderLayout.SOUTH);
            
            paletteGrid.add(tilePanel);
        }
        
        JScrollPane scrollPane = new JScrollPane(paletteGrid);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Info label
        JLabel infoLabel = new JLabel("<html><b>Red border = Solid tile</b><br>Hover for details</html>");
        infoLabel.setFont(infoLabel.getFont().deriveFont(10f));
        panel.add(infoLabel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createEntityInspectorTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(5, 5, 5, 5));
        
        entityInfoArea = new JTextArea();
        entityInfoArea.setEditable(false);
        entityInfoArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        entityInfoArea.setLineWrap(true);
        entityInfoArea.setWrapStyleWord(true);
        
        JScrollPane scrollPane = new JScrollPane(entityInfoArea);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        JButton refreshBtn = new JButton("Refresh Entity List");
        refreshBtn.addActionListener(e -> updateEntityInspector());
        panel.add(refreshBtn, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private void updateEntityInspector() {
        if (levelConfig == null) {
            entityInfoArea.setText("No level config loaded.");
            return;
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("=== ").append(currentLevel.toUpperCase()).append(" ===\n\n");
        
        // Player spawn
        Point playerSpawn = levelConfig.getPlayerSpawn();
        if (playerSpawn != null) {
            sb.append("PLAYER SPAWN:\n");
            sb.append("  Pixel: (").append(playerSpawn.x).append(", ").append(playerSpawn.y).append(")\n");
            sb.append("  Tile: (").append(playerSpawn.x / GAME_TILE_SIZE).append(", ").append(playerSpawn.y / GAME_TILE_SIZE).append(")\n\n");
        }
        
        // Toy spawn
        Point toySpawn = levelConfig.getToySpawn();
        if (toySpawn != null) {
            sb.append("TOY SPAWN:\n");
            sb.append("  Pixel: (").append(toySpawn.x).append(", ").append(toySpawn.y).append(")\n");
            sb.append("  Tile: (").append(toySpawn.x / GAME_TILE_SIZE).append(", ").append(toySpawn.y / GAME_TILE_SIZE).append(")\n\n");
        }
        
        // Snails
        List<SnailLocationData> snails = levelConfig.getSnailLocations();
        if (snails != null && !snails.isEmpty()) {
            sb.append("SNAILS (").append(snails.size()).append("):\n");
            int i = 1;
            for (SnailLocationData snail : snails) {
                Point p = snail.getPosition();
                sb.append("  #").append(i++).append(": ");
                sb.append("Tile(").append(p.x / GAME_TILE_SIZE).append(",").append(p.y / GAME_TILE_SIZE).append(") ");
                sb.append("[").append(snail.getDialogue().length).append(" lines]");
                if (snail.getDescription() != null) {
                    sb.append(" - ").append(snail.getDescription());
                }
                sb.append("\n");
            }
            sb.append("\n");
        }
        
        // Spiders
        List<SpiderPatrolData> spiders = levelConfig.getSpiderPatrols();
        if (spiders != null && !spiders.isEmpty()) {
            sb.append("SPIDERS (").append(spiders.size()).append("):\n");
            int i = 1;
            for (SpiderPatrolData spider : spiders) {
                sb.append("  #").append(i++).append(": ");
                sb.append(spider.getWaypoints().size()).append(" waypoints");
                if (spider.getDescription() != null) {
                    sb.append(" - ").append(spider.getDescription());
                }
                sb.append("\n");
            }
            sb.append("\n");
        }
        
        // Food
        List<FoodSpawnData> food = levelConfig.getFoodSpawns();
        if (food != null && !food.isEmpty()) {
            long berries = food.stream().filter(f -> !f.isSpeedBoostFood()).count();
            long seeds = food.stream().filter(f -> f.isSpeedBoostFood()).count();
            sb.append("FOOD (").append(food.size()).append("):\n");
            sb.append("  Berries: ").append(berries).append("\n");
            sb.append("  Energy Seeds: ").append(seeds).append("\n\n");
        }
        
        // Tripwires
        List<Point> tripwires = levelConfig.getTripWirePositions();
        if (tripwires != null && !tripwires.isEmpty()) {
            sb.append("TRIPWIRES (").append(tripwires.size()).append("):\n");
            for (Point p : tripwires) {
                sb.append("  Tile(").append(p.x / GAME_TILE_SIZE).append(",").append(p.y / GAME_TILE_SIZE).append(")\n");
            }
            sb.append("\n");
        }
        
        // Mechanics
        MechanicsConfig mechanics = levelConfig.getMechanicsEnabled();
        if (mechanics != null) {
            sb.append("MECHANICS:\n");
            sb.append("  Dash: ").append(mechanics.isDashEnabled() ? "YES" : "NO").append("\n");
            sb.append("  Toy: ").append(mechanics.isToyEnabled() ? "YES" : "NO").append("\n");
            sb.append("  Speed Boost Food: ").append(mechanics.isSpeedBoostFoodEnabled() ? "YES" : "NO").append("\n");
        }
        
        entityInfoArea.setText(sb.toString());
        entityInfoArea.setCaretPosition(0);
    }
    
    private JPanel createStatsTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(5, 5, 5, 5));
        
        JTextArea statsArea = new JTextArea();
        statsArea.setEditable(false);
        statsArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        
        JScrollPane scrollPane = new JScrollPane(statsArea);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        JButton refreshBtn = new JButton("Calculate Stats");
        refreshBtn.addActionListener(e -> {
            if (mapData == null) {
                statsArea.setText("No map loaded.");
                return;
            }
            
            // Count tiles
            Map<Integer, Integer> tileCounts = new HashMap<>();
            int solidCount = 0;
            int walkableCount = 0;
            
            for (int row = 0; row < mapHeight; row++) {
                for (int col = 0; col < mapWidth; col++) {
                    int id = mapData[row][col];
                    tileCounts.merge(id, 1, Integer::sum);
                    if (TileConstants.isSolidTile(id)) {
                        solidCount++;
                    } else {
                        walkableCount++;
                    }
                }
            }
            
            StringBuilder sb = new StringBuilder();
            sb.append("=== MAP STATISTICS ===\n\n");
            sb.append("Dimensions: ").append(mapWidth).append(" x ").append(mapHeight).append(" tiles\n");
            sb.append("Total tiles: ").append(mapWidth * mapHeight).append("\n");
            sb.append("Solid tiles: ").append(solidCount).append(" (").append(String.format("%.1f", solidCount * 100.0 / (mapWidth * mapHeight))).append("%)\n");
            sb.append("Walkable tiles: ").append(walkableCount).append(" (").append(String.format("%.1f", walkableCount * 100.0 / (mapWidth * mapHeight))).append("%)\n\n");
            
            sb.append("Pixel dimensions:\n");
            sb.append("  Width: ").append(mapWidth * GAME_TILE_SIZE).append("px\n");
            sb.append("  Height: ").append(mapHeight * GAME_TILE_SIZE).append("px\n\n");
            
            sb.append("TILE USAGE:\n");
            tileCounts.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .forEach(e2 -> {
                    int id = e2.getKey();
                    int count = e2.getValue();
                    sb.append("  ID ").append(String.format("%2d", id)).append(": ");
                    sb.append(String.format("%4d", count)).append(" tiles");
                    if (TileConstants.isSolidTile(id)) sb.append(" [SOLID]");
                    sb.append("\n");
                });
            
            statsArea.setText(sb.toString());
            statsArea.setCaretPosition(0);
        });
        panel.add(refreshBtn, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private void addLegendItem(JPanel panel, Color color, String text) {
        JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        JPanel colorBox = new JPanel();
        colorBox.setBackground(color);
        colorBox.setPreferredSize(new Dimension(16, 16));
        colorBox.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        item.add(colorBox);
        item.add(new JLabel(text));
        panel.add(item);
    }
    
    private JPanel createStatusBar() {
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(new EmptyBorder(3, 10, 3, 10));
        
        statusLabel = new JLabel("Ready");
        statusBar.add(statusLabel, BorderLayout.WEST);
        
        coordLabel = new JLabel("Tile: (-, -)  Pixel: (-, -)");
        statusBar.add(coordLabel, BorderLayout.EAST);
        
        return statusBar;
    }
    
    private void setupKeyBindings() {
        // R key to reload
        canvas.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_R, 0), "reload");
        canvas.getActionMap().put("reload", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                reloadCurrentLevel();
            }
        });
        
        // F key to fit view
        canvas.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_F, 0), "fit");
        canvas.getActionMap().put("fit", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                fitToView();
            }
        });
        
        // Arrow keys to pan
        String[] dirs = {"UP", "DOWN", "LEFT", "RIGHT"};
        int[] dxs = {0, 0, -20, 20};
        int[] dys = {-20, 20, 0, 0};
        for (int i = 0; i < dirs.length; i++) {
            final int dx = dxs[i];
            final int dy = dys[i];
            canvas.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(dirs[i]), "pan" + dirs[i]);
            canvas.getActionMap().put("pan" + dirs[i], new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    panX += dx;
                    panY += dy;
                    canvas.repaint();
                }
            });
        }
        
        // Number keys 1-5 to quick-switch levels
        for (int level = 1; level <= 5; level++) {
            final int lvl = level;
            canvas.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_0 + level, 0), "level" + level);
            canvas.getActionMap().put("level" + level, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    levelSelector.setSelectedItem("level" + lvl);
                }
            });
        }
        
        // T key for test level
        canvas.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_T, 0), "levelTest");
        canvas.getActionMap().put("levelTest", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                levelSelector.setSelectedItem("level_test");
            }
        });
        
        // M key to toggle measure mode
        canvas.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_M, 0), "measure");
        canvas.getActionMap().put("measure", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                measureMode = !measureMode;
                measureStart = null;
                measureEnd = null;
                canvas.setCursor(measureMode ? Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR) : Cursor.getDefaultCursor());
                canvas.repaint();
                updateStatus(measureMode ? "Measure mode ON - Click to set points" : "Measure mode OFF");
            }
        });
        
        // Escape to clear measurement / exit measure mode
        canvas.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "clearMeasure");
        canvas.getActionMap().put("clearMeasure", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                measureMode = false;
                measureStart = null;
                measureEnd = null;
                canvas.setCursor(Cursor.getDefaultCursor());
                canvas.repaint();
                updateStatus("Ready");
            }
        });
    }
    
    // ========== LEVEL LOADING ==========
    private void loadLevel(String levelName) {
        this.currentLevel = levelName;
        loadMapFromFile(levelName);
        loadLevelConfig(levelName);
        panX = 0;
        panY = 0;
        measureStart = null;
        measureEnd = null;
        canvas.repaint();
        updateStatus("Loaded: " + levelName + " (" + mapWidth + "x" + mapHeight + " tiles)");
        
        // Auto-update entity inspector if visible
        if (entityInfoArea != null) {
            updateEntityInspector();
        }
    }
    
    private void loadMapFromFile(String levelName) {
        String filePath = "/res/maps/" + levelName + ".txt";
        List<List<Integer>> mapRows = new ArrayList<>();
        
        try {
            InputStream is = getClass().getResourceAsStream(filePath);
            if (is == null) {
                // Try loading from file system for live reload
                Path path = Paths.get("res/maps/" + levelName + ".txt");
                if (Files.exists(path)) {
                    is = new FileInputStream(path.toFile());
                } else {
                    updateStatus("ERROR: Map file not found: " + levelName);
                    return;
                }
            }
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                // Stop reading tiles when we hit a comment/entity section
                if (line.trim().startsWith("#")) break;
                List<Integer> row = new ArrayList<>();
                String[] numbers = line.trim().split("\\s+");
                for (String num : numbers) {
                    row.add(Integer.parseInt(num));
                }
                mapRows.add(row);
            }
            reader.close();
            
        } catch (Exception e) {
            updateStatus("ERROR loading map: " + e.getMessage());
            return;
        }
        
        mapHeight = mapRows.size();
        mapWidth = mapRows.isEmpty() ? 0 : mapRows.get(0).size();
        mapData = new int[mapHeight][mapWidth];
        for (int row = 0; row < mapHeight; row++) {
            for (int col = 0; col < mapWidth; col++) {
                mapData[row][col] = mapRows.get(row).get(col);
            }
        }
    }
    
    /**
     * Load level config by parsing entity data directly from the .txt map file.
     * Entity sections appear after the tile grid, marked with # headers.
     * Falls back to hardcoded Java config if no entity data found in file.
     */
    private void loadLevelConfig(String levelName) {
        // Try parsing entity data from the .txt file
        try {
            Path path = Paths.get("res/maps/" + levelName + ".txt");
            if (Files.exists(path)) {
                TxtBackedLevelConfig txtConfig = TxtBackedLevelConfig.parse(path, levelName);
                if (txtConfig != null && txtConfig.hasEntityData()) {
                    levelConfig = txtConfig;
                    System.out.println("[MapPreview] Loaded entity data from: " + levelName + ".txt");
                    return;
                }
            }
        } catch (Exception e) {
            System.err.println("[MapPreview] Error parsing entity data from txt: " + e.getMessage());
        }
        // Fall back to hardcoded Java config
        try {
            levelConfig = LevelConfigFactory.getConfig(levelName);
        } catch (Exception e) {
            levelConfig = null;
            System.err.println("Could not load level config for: " + levelName);
        }
    }

    /**
     * Reads entity data (spiders, snails, food, etc.) from # sections
     * at the bottom of a .txt map file. This enables live-reload of
     * entity positions by simply editing and saving the .txt file.
     *
     * Supported sections:
     *   # PLAYER_SPAWN
     *   pixelX pixelY
     *
     *   # TOY_SPAWN
     *   pixelX pixelY
     *
     *   # SPIDERS
     *   rectangle left top right bottom [description]
     *   horizontal startX endX y [description]
     *   vertical x startY endY [description]
     *   custom x1,y1 x2,y2 x3,y3 ... [description]
     *
     *   # SNAILS
     *   pixelX pixelY interaction(true/false) dialogue line 1|line 2|line 3
     *
     *   # FOOD
     *   berry tileX tileY [description]
     *   energy_seed tileX tileY [description]
     *
     *   # TRIPWIRES
     *   pixelX pixelY
     *
     *   # MECHANICS
     *   dash true/false
     *   toy true/false
     *   speedboost true/false
     *   tripwires true/false
     */
    private static class TxtBackedLevelConfig implements LevelConfig {
        private String levelName;
        private Point playerSpawn;
        private Point toySpawn;
        private List<SpiderPatrolData> spiders = new ArrayList<>();
        private List<SnailLocationData> snails = new ArrayList<>();
        private List<FoodSpawnData> food = new ArrayList<>();
        private List<Point> tripwires = new ArrayList<>();
        private MechanicsConfig mechanics = new MechanicsConfig();

        public boolean hasEntityData() {
            return !spiders.isEmpty() || !snails.isEmpty() || !food.isEmpty()
                || !tripwires.isEmpty() || playerSpawn != null || toySpawn != null;
        }

        @Override public String getLevelName() { return levelName; }
        @Override public Point getPlayerSpawn() { return playerSpawn; }
        @Override public Point getToySpawn() { return toySpawn; }
        @Override public MechanicsConfig getMechanicsEnabled() { return mechanics; }
        @Override public List<SpiderPatrolData> getSpiderPatrols() { return spiders; }
        @Override public List<SnailLocationData> getSnailLocations() { return snails; }
        @Override public List<FoodSpawnData> getFoodSpawns() { return food; }
        @Override public List<Point> getTripWirePositions() { return tripwires; }

        /**
         * Parse entity sections from a .txt map file.
         * Returns null if there are no # sections in the file.
         */
        public static TxtBackedLevelConfig parse(Path filePath, String levelName) throws IOException {
            List<String> allLines = Files.readAllLines(filePath);
            TxtBackedLevelConfig config = new TxtBackedLevelConfig();
            config.levelName = levelName;

            // Find the first # line (start of entity sections)
            int sectionStart = -1;
            for (int i = 0; i < allLines.size(); i++) {
                if (allLines.get(i).trim().startsWith("#")) {
                    sectionStart = i;
                    break;
                }
            }
            if (sectionStart == -1) return null; // No entity sections

            String currentSection = "";
            for (int i = sectionStart; i < allLines.size(); i++) {
                String line = allLines.get(i).trim();
                if (line.isEmpty()) continue;

                // Section header
                if (line.startsWith("#")) {
                    currentSection = line.substring(1).trim().toUpperCase();
                    continue;
                }

                try {
                    switch (currentSection) {
                        case "PLAYER_SPAWN":
                            parsePlayerSpawn(config, line);
                            break;
                        case "TOY_SPAWN":
                            parseToySpawn(config, line);
                            break;
                        case "SPIDERS":
                            parseSpider(config, line);
                            break;
                        case "SNAILS":
                            parseSnail(config, line);
                            break;
                        case "FOOD":
                            parseFood(config, line);
                            break;
                        case "TRIPWIRES":
                            parseTripwire(config, line);
                            break;
                        case "MECHANICS":
                            parseMechanic(config, line);
                            break;
                    }
                } catch (Exception e) {
                    System.err.println("[MapPreview] Warning: could not parse line " + (i+1) + ": " + line + " (" + e.getMessage() + ")");
                }
            }
            return config;
        }

        private static void parsePlayerSpawn(TxtBackedLevelConfig config, String line) {
            String[] parts = line.split("\\s+");
            config.playerSpawn = new Point(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        }

        private static void parseToySpawn(TxtBackedLevelConfig config, String line) {
            if (line.equalsIgnoreCase("none")) return;
            String[] parts = line.split("\\s+");
            config.toySpawn = new Point(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        }

        private static void parseSpider(TxtBackedLevelConfig config, String line) {
            String[] parts = line.split("\\s+", 2);
            String type = parts[0].toLowerCase();
            String rest = parts.length > 1 ? parts[1] : "";
            String[] args = rest.split("\\s+");

            SpiderPatrolData patrol = null;
            String description = null;

            switch (type) {
                case "rectangle": {
                    int left = Integer.parseInt(args[0]);
                    int top = Integer.parseInt(args[1]);
                    int right = Integer.parseInt(args[2]);
                    int bottom = Integer.parseInt(args[3]);
                    patrol = SpiderPatrolData.rectangle(left, top, right, bottom);
                    if (args.length > 4) {
                        description = joinFrom(args, 4);
                    }
                    break;
                }
                case "horizontal": {
                    int startX = Integer.parseInt(args[0]);
                    int endX = Integer.parseInt(args[1]);
                    int y = Integer.parseInt(args[2]);
                    patrol = SpiderPatrolData.horizontal(startX, endX, y);
                    if (args.length > 3) {
                        description = joinFrom(args, 3);
                    }
                    break;
                }
                case "vertical": {
                    int x = Integer.parseInt(args[0]);
                    int startY = Integer.parseInt(args[1]);
                    int endY = Integer.parseInt(args[2]);
                    patrol = SpiderPatrolData.vertical(x, startY, endY);
                    if (args.length > 3) {
                        description = joinFrom(args, 3);
                    }
                    break;
                }
                case "custom": {
                    SpiderPatrolData.CustomBuilder builder = SpiderPatrolData.custom();
                    int waypointEnd = 0;
                    for (int j = 0; j < args.length; j++) {
                        if (args[j].contains(",")) {
                            String[] xy = args[j].split(",");
                            builder.addPoint(Integer.parseInt(xy[0]), Integer.parseInt(xy[1]));
                            waypointEnd = j + 1;
                        } else {
                            break;
                        }
                    }
                    patrol = builder.build();
                    if (waypointEnd < args.length) {
                        description = joinFrom(args, waypointEnd);
                    }
                    break;
                }
            }

            if (patrol != null) {
                if (description != null) patrol.describe(description);
                config.spiders.add(patrol);
            }
        }

        private static void parseSnail(TxtBackedLevelConfig config, String line) {
            // Format: pixelX pixelY interaction(true/false) dialogue1|dialogue2|...
            String[] parts = line.split("\\s+", 4);
            int px = Integer.parseInt(parts[0]);
            int py = Integer.parseInt(parts[1]);
            boolean interaction = Boolean.parseBoolean(parts[2]);
            SnailLocationData snail = SnailLocationData.at(px, py);
            if (parts.length > 3) {
                String[] dialogueLines = parts[3].split("\\|");
                snail.withDialogue(dialogueLines);
            }
            if (interaction) snail.requiresInteraction();
            else snail.autoAdvance();
            config.snails.add(snail);
        }

        private static void parseFood(TxtBackedLevelConfig config, String line) {
            // Format: type tileX tileY [description]
            String[] parts = line.split("\\s+", 4);
            String type = parts[0].toLowerCase();
            int tx = Integer.parseInt(parts[1]);
            int ty = Integer.parseInt(parts[2]);
            FoodSpawnData fd;
            if (type.equals("energy_seed")) {
                fd = FoodSpawnData.energySeed(tx, ty);
            } else {
                fd = FoodSpawnData.berry(tx, ty);
            }
            if (parts.length > 3) fd.describe(parts[3]);
            config.food.add(fd);
        }

        private static void parseTripwire(TxtBackedLevelConfig config, String line) {
            if (line.equalsIgnoreCase("none")) return;
            String[] parts = line.split("\\s+");
            config.tripwires.add(new Point(Integer.parseInt(parts[0]), Integer.parseInt(parts[1])));
        }

        private static void parseMechanic(TxtBackedLevelConfig config, String line) {
            String[] parts = line.split("\\s+");
            String key = parts[0].toLowerCase();
            boolean val = Boolean.parseBoolean(parts[1]);
            switch (key) {
                case "dash":      if (val) config.mechanics.enableDash(); break;
                case "toy":       if (val) config.mechanics.enableToy(); break;
                case "speedboost": if (val) config.mechanics.enableSpeedBoostFood(); break;
                case "tripwires": if (val) config.mechanics.enableTripWires(); break;
            }
        }

        private static String joinFrom(String[] parts, int startIndex) {
            StringBuilder sb = new StringBuilder();
            for (int i = startIndex; i < parts.length; i++) {
                if (i > startIndex) sb.append(" ");
                sb.append(parts[i]);
            }
            return sb.toString();
        }
    }
    
    private void reloadCurrentLevel() {
        loadLevel(currentLevel);
        updateStatus("Reloaded: " + currentLevel);
    }
    
    private void fitToView() {
        if (mapWidth == 0 || mapHeight == 0) return;
        
        int availableWidth = canvas.getWidth() - 40;
        int availableHeight = canvas.getHeight() - 40;
        
        int fitW = availableWidth / mapWidth;
        int fitH = availableHeight / mapHeight;
        previewTileSize = Math.max(MIN_TILE_SIZE, Math.min(MAX_TILE_SIZE, Math.min(fitW, fitH)));
        
        panX = 0;
        panY = 0;
        canvas.repaint();
    }
    
    private void updateStatus(String message) {
        statusLabel.setText(message);
    }
    
    // ========== FILE WATCHER FOR LIVE RELOAD ==========
    private void startFileWatcher() {
        if (fileWatcher != null) return;
        
        fileWatcher = new FileWatcher();
        fileWatcher.start();
    }
    
    private void stopFileWatcher() {
        if (fileWatcher != null) {
            fileWatcher.stopWatching();
            fileWatcher = null;
        }
    }
    
    private class FileWatcher extends Thread {
        private volatile boolean running = true;
        private long lastModifiedTxt = 0;
        private long lastModifiedJson = 0;
        
        @Override
        public void run() {
            while (running) {
                try {
                    Thread.sleep(1000);  // Check every second
                    if (!liveReload) continue;
                    
                    boolean changed = false;
                    
                    // Watch .txt map file for tile changes
                    Path mapPath = Paths.get("res/maps/" + currentLevel + ".txt");
                    if (Files.exists(mapPath)) {
                        long mod = Files.getLastModifiedTime(mapPath).toMillis();
                        if (mod > lastModifiedTxt && lastModifiedTxt != 0) {
                            changed = true;
                        }
                        lastModifiedTxt = mod;
                    }
                    
                    // Watch .json file for entity data changes (spiders, snails, food, etc.)
                    Path jsonPath = Paths.get("res/maps/" + currentLevel + ".json");
                    if (Files.exists(jsonPath)) {
                        long mod = Files.getLastModifiedTime(jsonPath).toMillis();
                        if (mod > lastModifiedJson && lastModifiedJson != 0) {
                            changed = true;
                        }
                        lastModifiedJson = mod;
                    }
                    
                    if (changed) {
                        SwingUtilities.invokeLater(() -> {
                            reloadCurrentLevel();
                            updateStatus("Live reloaded: " + currentLevel);
                        });
                    }
                } catch (Exception e) {
                    // Ignore
                }
            }
        }
        
        public void stopWatching() {
            running = false;
        }
    }
    
    // ========== EXPORT TO PNG ==========
    private void exportToPNG() {
        if (mapData == null) return;
        
        int imgWidth = mapWidth * previewTileSize;
        int imgHeight = mapHeight * previewTileSize;
        java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(
            imgWidth, imgHeight, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Draw with no pan offset for export
        int oldPanX = panX, oldPanY = panY;
        panX = 0;
        panY = 0;
        canvas.paintMap(g, imgWidth, imgHeight);
        panX = oldPanX;
        panY = oldPanY;
        
        g.dispose();
        
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(currentLevel + "_preview.png"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                javax.imageio.ImageIO.write(image, "PNG", chooser.getSelectedFile());
                updateStatus("Exported: " + chooser.getSelectedFile().getName());
            } catch (IOException e) {
                updateStatus("Export failed: " + e.getMessage());
            }
        }
    }
    
    // ========== MAP CANVAS ==========
    private class MapCanvas extends JPanel {
        
        public MapCanvas() {
            setBackground(Color.DARK_GRAY);
            
            // Mouse wheel zoom
            addMouseWheelListener(e -> {
                int delta = -e.getWheelRotation();
                previewTileSize = Math.max(MIN_TILE_SIZE, Math.min(MAX_TILE_SIZE, previewTileSize + delta * 2));
                repaint();
            });
            
            // Right-click drag to pan
            MouseAdapter mouseAdapter = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (measureMode && SwingUtilities.isLeftMouseButton(e)) {
                        int tileX = (e.getX() + panX) / previewTileSize;
                        int tileY = (e.getY() + panY) / previewTileSize;
                        if (measureStart == null) {
                            measureStart = new Point(tileX, tileY);
                            updateStatus("Measure: Start at (" + tileX + ", " + tileY + ") - Click to set end point");
                        } else {
                            measureEnd = new Point(tileX, tileY);
                            // Calculate distance
                            int dx = Math.abs(measureEnd.x - measureStart.x);
                            int dy = Math.abs(measureEnd.y - measureStart.y);
                            double diag = Math.sqrt(dx * dx + dy * dy);
                            int pixelDx = dx * GAME_TILE_SIZE;
                            int pixelDy = dy * GAME_TILE_SIZE;
                            double pixelDiag = diag * GAME_TILE_SIZE;
                            updateStatus(String.format("Distance: %d×%d tiles (%.1f diag) | %d×%d px (%.0f diag)", 
                                dx, dy, diag, pixelDx, pixelDy, pixelDiag));
                        }
                        repaint();
                        return;
                    }
                    if (SwingUtilities.isRightMouseButton(e)) {
                        isPanning = true;
                        lastMousePos = e.getPoint();
                    }
                }
                
                @Override
                public void mouseReleased(MouseEvent e) {
                    isPanning = false;
                }
                
                @Override
                public void mouseDragged(MouseEvent e) {
                    if (isPanning && lastMousePos != null) {
                        int dx = e.getX() - lastMousePos.x;
                        int dy = e.getY() - lastMousePos.y;
                        panX -= dx;
                        panY -= dy;
                        lastMousePos = e.getPoint();
                        repaint();
                    }
                }
                
                @Override
                public void mouseMoved(MouseEvent e) {
                    updateCoordinates(e.getX(), e.getY());
                }
            };
            addMouseListener(mouseAdapter);
            addMouseMotionListener(mouseAdapter);
        }
        
        private void updateCoordinates(int screenX, int screenY) {
            int tileX = (screenX + panX) / previewTileSize;
            int tileY = (screenY + panY) / previewTileSize;
            int pixelX = tileX * GAME_TILE_SIZE + GAME_TILE_SIZE / 2;
            int pixelY = tileY * GAME_TILE_SIZE + GAME_TILE_SIZE / 2;
            
            String tileInfo = "";
            if (mapData != null && tileY >= 0 && tileY < mapHeight && tileX >= 0 && tileX < mapWidth) {
                int tileId = mapData[tileY][tileX];
                tileInfo = " | ID: " + tileId + (TileConstants.isSolidTile(tileId) ? " (solid)" : "");
            }
            
            coordLabel.setText(String.format("Tile: (%d, %d)  Pixel: (%d, %d)%s", 
                tileX, tileY, pixelX, pixelY, tileInfo));
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            paintMap((Graphics2D) g, getWidth(), getHeight());
        }
        
        public void paintMap(Graphics2D g, int width, int height) {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            if (mapData == null) {
                g.setColor(Color.WHITE);
                g.drawString("No map loaded", 50, 50);
                return;
            }
            
            // Draw tiles
            for (int row = 0; row < mapHeight; row++) {
                for (int col = 0; col < mapWidth; col++) {
                    int tileId = mapData[row][col];
                    int x = col * previewTileSize - panX;
                    int y = row * previewTileSize - panY;
                    
                    // Skip if off-screen
                    if (x + previewTileSize < 0 || x > width || y + previewTileSize < 0 || y > height) {
                        continue;
                    }
                    
                    // Get tile color
                    Color color = TILE_COLORS.getOrDefault(tileId, new Color(100, 80, 70));
                    g.setColor(color);
                    g.fillRect(x, y, previewTileSize, previewTileSize);
                    
                    // Mark solid tiles
                    if (TileConstants.isSolidTile(tileId)) {
                        g.setColor(new Color(255, 255, 255, 40));
                        g.drawRect(x, y, previewTileSize - 1, previewTileSize - 1);
                    }
                }
            }
            
            // Draw grid
            if (showGrid) {
                g.setColor(new Color(255, 255, 255, 30));
                for (int row = 0; row <= mapHeight; row++) {
                    int y = row * previewTileSize - panY;
                    g.drawLine(-panX, y, mapWidth * previewTileSize - panX, y);
                }
                for (int col = 0; col <= mapWidth; col++) {
                    int x = col * previewTileSize - panX;
                    g.drawLine(x, -panY, x, mapHeight * previewTileSize - panY);
                }
            }
            
            // Draw coordinate labels at edges
            if (showCoordinates && previewTileSize >= 12) {
                g.setFont(new Font("Monospaced", Font.PLAIN, 9));
                g.setColor(new Color(200, 200, 200, 150));
                for (int col = 0; col < mapWidth; col += 5) {
                    int x = col * previewTileSize - panX + 2;
                    g.drawString(String.valueOf(col), x, 10);
                }
                for (int row = 0; row < mapHeight; row += 5) {
                    int y = row * previewTileSize - panY + previewTileSize / 2;
                    g.drawString(String.valueOf(row), 2, y);
                }
            }
            
            // Draw entities from level config
            if (levelConfig != null) {
                drawEntities(g);
            }
            
            // Draw measurement line
            if (measureMode && measureStart != null) {
                g.setColor(Color.YELLOW);
                g.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{5, 5}, 0));
                
                int x1 = measureStart.x * previewTileSize + previewTileSize / 2 - panX;
                int y1 = measureStart.y * previewTileSize + previewTileSize / 2 - panY;
                
                if (measureEnd != null) {
                    int x2 = measureEnd.x * previewTileSize + previewTileSize / 2 - panX;
                    int y2 = measureEnd.y * previewTileSize + previewTileSize / 2 - panY;
                    g.drawLine(x1, y1, x2, y2);
                    
                    // Draw endpoints
                    g.setStroke(new BasicStroke(1));
                    g.fillOval(x1 - 5, y1 - 5, 10, 10);
                    g.fillOval(x2 - 5, y2 - 5, 10, 10);
                    
                    // Draw distance label
                    int dx = Math.abs(measureEnd.x - measureStart.x);
                    int dy = Math.abs(measureEnd.y - measureStart.y);
                    double diag = Math.sqrt(dx * dx + dy * dy);
                    String label = String.format("%d×%d (%.1f)", dx, dy, diag);
                    g.setFont(new Font("SansSerif", Font.BOLD, 12));
                    int labelX = (x1 + x2) / 2;
                    int labelY = (y1 + y2) / 2 - 10;
                    g.setColor(Color.BLACK);
                    g.drawString(label, labelX + 1, labelY + 1);
                    g.setColor(Color.YELLOW);
                    g.drawString(label, labelX, labelY);
                } else {
                    // Just draw start point
                    g.setStroke(new BasicStroke(1));
                    g.fillOval(x1 - 6, y1 - 6, 12, 12);
                    g.setColor(Color.BLACK);
                    g.drawOval(x1 - 6, y1 - 6, 12, 12);
                }
            }
        }
        
        private void drawEntities(Graphics2D g) {
            // === SNAILS ===
            if (showSnails && levelConfig.getSnailLocations() != null) {
                for (SnailLocationData snail : levelConfig.getSnailLocations()) {
                    Point pos = snail.getPosition();
                    int x = (pos.x / GAME_TILE_SIZE) * previewTileSize - panX;
                    int y = (pos.y / GAME_TILE_SIZE) * previewTileSize - panY;
                    
                    g.setColor(new Color(0, 255, 255, 200));
                    g.fillOval(x - 6, y - 6, 12, 12);
                    g.setColor(Color.WHITE);
                    g.drawOval(x - 6, y - 6, 12, 12);
                    
                    // Draw dialogue count
                    if (snail.getDialogue() != null && previewTileSize >= 10) {
                        g.setFont(new Font("SansSerif", Font.BOLD, 10));
                        g.setColor(Color.WHITE);
                        g.drawString("" + snail.getDialogue().length, x + 8, y - 2);
                    }
                    
                    // Draw description tooltip
                    if (snail.getDescription() != null && previewTileSize >= 16) {
                        g.setFont(new Font("SansSerif", Font.PLAIN, 9));
                        g.setColor(new Color(255, 255, 255, 180));
                        g.drawString(snail.getDescription(), x + 10, y + 4);
                    }
                }
            }
            
            // === SPIDERS ===
            if (showSpiders && levelConfig.getSpiderPatrols() != null) {
                int spiderIndex = 0;
                for (SpiderPatrolData patrol : levelConfig.getSpiderPatrols()) {
                    List<Point> waypoints = patrol.getWaypoints();
                    if (waypoints == null || waypoints.isEmpty()) continue;
                    
                    // Draw patrol path
                    g.setColor(new Color(255, 0, 0, 150));
                    g.setStroke(new BasicStroke(2));
                    for (int i = 0; i < waypoints.size() - 1; i++) {
                        Point p1 = waypoints.get(i);
                        Point p2 = waypoints.get(i + 1);
                        int x1 = p1.x * previewTileSize + previewTileSize / 2 - panX;
                        int y1 = p1.y * previewTileSize + previewTileSize / 2 - panY;
                        int x2 = p2.x * previewTileSize + previewTileSize / 2 - panX;
                        int y2 = p2.y * previewTileSize + previewTileSize / 2 - panY;
                        g.drawLine(x1, y1, x2, y2);
                    }
                    
                    // Draw waypoints
                    g.setStroke(new BasicStroke(1));
                    for (int i = 0; i < waypoints.size(); i++) {
                        Point p = waypoints.get(i);
                        int x = p.x * previewTileSize + previewTileSize / 2 - panX;
                        int y = p.y * previewTileSize + previewTileSize / 2 - panY;
                        
                        g.setColor(i == 0 ? Color.RED : new Color(255, 100, 100));
                        g.fillRect(x - 4, y - 4, 8, 8);
                        g.setColor(Color.WHITE);
                        g.drawRect(x - 4, y - 4, 8, 8);
                    }
                    
                    // Draw spider number at start
                    Point start = waypoints.get(0);
                    int sx = start.x * previewTileSize + previewTileSize / 2 - panX;
                    int sy = start.y * previewTileSize + previewTileSize / 2 - panY;
                    g.setFont(new Font("SansSerif", Font.BOLD, 10));
                    g.setColor(Color.WHITE);
                    g.drawString("S" + (spiderIndex + 1), sx + 6, sy - 6);
                    
                    // Draw description
                    if (patrol.getDescription() != null && previewTileSize >= 16) {
                        g.setFont(new Font("SansSerif", Font.PLAIN, 9));
                        g.setColor(new Color(255, 200, 200, 180));
                        g.drawString(patrol.getDescription(), sx + 20, sy - 6);
                    }
                    
                    spiderIndex++;
                }
            }
            
            // === FOOD ===
            if (showFood && levelConfig.getFoodSpawns() != null) {
                for (FoodSpawnData food : levelConfig.getFoodSpawns()) {
                    Point pos = food.getTilePosition();
                    int x = pos.x * previewTileSize + previewTileSize / 2 - panX;
                    int y = pos.y * previewTileSize + previewTileSize / 2 - panY;
                    
                    // Different colors for food types
                    if (food.getType().name().equals("ENERGY_SEED")) {
                        g.setColor(new Color(100, 255, 100, 200));
                    } else {
                        g.setColor(new Color(255, 255, 0, 200));
                    }
                    g.fillOval(x - 5, y - 5, 10, 10);
                    g.setColor(Color.WHITE);
                    g.drawOval(x - 5, y - 5, 10, 10);
                }
            }
            
            // === TRIPWIRES ===
            if (showTripwires && levelConfig.getTripWirePositions() != null) {
                for (Point pos : levelConfig.getTripWirePositions()) {
                    int x = (pos.x / GAME_TILE_SIZE) * previewTileSize + previewTileSize / 2 - panX;
                    int y = (pos.y / GAME_TILE_SIZE) * previewTileSize + previewTileSize / 2 - panY;
                    
                    g.setColor(new Color(255, 165, 0, 200));
                    g.setStroke(new BasicStroke(3));
                    g.drawLine(x - 8, y, x + 8, y);
                    g.setStroke(new BasicStroke(1));
                }
            }
            
            // === PLAYER SPAWN ===
            if (showPlayerSpawn && levelConfig.getPlayerSpawn() != null) {
                Point pos = levelConfig.getPlayerSpawn();
                int x = (pos.x / GAME_TILE_SIZE) * previewTileSize + previewTileSize / 2 - panX;
                int y = (pos.y / GAME_TILE_SIZE) * previewTileSize + previewTileSize / 2 - panY;
                
                g.setColor(new Color(50, 150, 255, 200));
                g.fillOval(x - 8, y - 8, 16, 16);
                g.setColor(Color.WHITE);
                g.setStroke(new BasicStroke(2));
                g.drawOval(x - 8, y - 8, 16, 16);
                g.setStroke(new BasicStroke(1));
                
                g.setFont(new Font("SansSerif", Font.BOLD, 10));
                g.drawString("P", x - 3, y + 4);
            }
            
            // === TOY SPAWN ===
            if (showToySpawn && levelConfig.getToySpawn() != null) {
                Point pos = levelConfig.getToySpawn();
                int x = (pos.x / GAME_TILE_SIZE) * previewTileSize + previewTileSize / 2 - panX;
                int y = (pos.y / GAME_TILE_SIZE) * previewTileSize + previewTileSize / 2 - panY;
                
                g.setColor(new Color(255, 0, 255, 200));
                g.fillRect(x - 6, y - 6, 12, 12);
                g.setColor(Color.WHITE);
                g.drawRect(x - 6, y - 6, 12, 12);
                
                g.setFont(new Font("SansSerif", Font.BOLD, 10));
                g.drawString("T", x - 3, y + 4);
            }
        }
    }
    
    @Override
    public void dispose() {
        stopFileWatcher();
        super.dispose();
    }
}
