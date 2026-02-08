package com.buglife.engine.editor;

import com.buglife.config.TileConstants;
import com.buglife.engine.editor.data.LevelData;
import com.buglife.engine.editor.data.LevelData.*;
import com.buglife.engine.editor.data.LevelDataIO;
import com.buglife.engine.editor.tools.EntityTools;
import com.buglife.engine.editor.tools.TileTools;
import com.buglife.engine.editor.tools.UndoManager;
import com.buglife.engine.editor.validation.LevelValidator;
import com.buglife.engine.editor.validation.LevelValidator.ValidationIssue;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Full-featured level editor with tile painting, entity editing, and validation.
 * Inspired by Unity/Godot tile map editors.
 */
public class LevelEditor extends JFrame {
    
    private static final int TILE_SIZE = 64;
    private static final int PALETTE_TILE_SIZE = 32;
    
    // Data
    private LevelData levelData;
    private String currentFilePath;
    private boolean modified;
    
    // Tools
    private TileTools tileTools;
    private EntityTools entityTools;
    private UndoManager undoManager;
    
    // UI Components
    private MapCanvas mapCanvas;
    private TilePalette tilePalette;
    private ValidationPanel validationPanel;
    private EntityPanel entityPanel;
    @SuppressWarnings("unused")
    private PropertiesPanel propertiesPanel;
    private JLabel statusLabel;
    private JLabel coordinateLabel;
    
    // View state
    private float zoomLevel = 1.0f;
    
    // Editing mode
    public enum EditMode { TILES, ENTITIES }
    private EditMode editMode = EditMode.TILES;
    
    // Tile sprites
    private Map<Integer, BufferedImage> tileSprites = new HashMap<>();
    private BufferedImage playerSprite, toySprite, snailSprite, spiderSprite;
    private BufferedImage berrySprite, seedSprite, tripwireSprite;
    
    public LevelEditor() {
        super("Lullaby Down Below - Level Editor");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(1400, 900);
        setLocationRelativeTo(null);
        
        // Initialize tools
        tileTools = new TileTools();
        entityTools = new EntityTools();
        undoManager = new UndoManager();
        
        // Create new empty level
        levelData = new LevelData("New Level", 30, 20);
        
        // Load sprites
        loadSprites();
        
        // Build UI
        buildUI();
        buildMenuBar();
        setupKeyBindings();
        
        // Handle close
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                handleExit();
            }
        });
        
        setVisible(true);
        updateTitle();
    }
    
    // ========== SPRITE LOADING ==========
    
    private void loadSprites() {
        // Load tile sprites with actual file names
        loadTileSprite(TileConstants.FLOOR, "floor.png");
        loadTileSprite(TileConstants.WALL, "wall.png");
        loadTileSprite(TileConstants.WALL_ALT, "wall_5.png");
        loadTileSprite(TileConstants.STICKY_FLOOR, "sticky_floor.png");
        loadTileSprite(TileConstants.BROKEN_TILE, "broken_tile.png");
        loadTileSprite(TileConstants.SHADOW_TILE, "shadow_tile.png");
        loadTileSprite(TileConstants.STAIN_1, "stain_1.png");
        loadTileSprite(TileConstants.STAIN_2, "stain_2.png");
        loadTileSprite(TileConstants.STAIN_3, "stain_3.png");
        loadTileSprite(TileConstants.STAIN_4, "stain_4.png");
        loadTileSprite(TileConstants.SACK_W1, "sack_w1.png");
        loadTileSprite(TileConstants.SACK_W2, "sack_w2.png");
        loadTileSprite(TileConstants.SACK_W3, "sack_w3.png");
        loadTileSprite(TileConstants.SACK_W4, "sack_w4.png");
        loadTileSprite(TileConstants.PLANK_1, "plank1.png");
        loadTileSprite(TileConstants.PLANK_2, "plank2.png");
        loadTileSprite(TileConstants.PLANK_3, "plank3.png");
        loadTileSprite(TileConstants.PLANK_4, "plank4.png");
        loadTileSprite(TileConstants.LADDER_1, "l1.png");
        loadTileSprite(TileConstants.LADDER_2, "l2.png");
        loadTileSprite(TileConstants.LADDER_3, "l3.png");
        loadTileSprite(TileConstants.LADDER_4, "l4.png");
        loadTileSprite(TileConstants.INTRO_TILE_1, "introtile1.png");
        loadTileSprite(TileConstants.INTRO_TILE_2, "introtile2.png");
        loadTileSprite(TileConstants.INTRO_TILE_3, "introtile3.png");
        loadTileSprite(TileConstants.INTRO_TILE_4, "introtile4.png");
        loadTileSprite(TileConstants.INTRO_TILE_5, "introtile5.png");
        loadTileSprite(TileConstants.INTRO_TILE_6, "introtile6.png");
        
        // Load entity sprites
        playerSprite = loadEntitySprite("player/idle.png", Color.CYAN, "P");
        toySprite = loadEntitySprite("items/toy.png", Color.YELLOW, "T");
        snailSprite = loadEntitySprite("snail/snail.png", Color.ORANGE, "S");
        spiderSprite = loadEntitySprite("spider/spider_idle.png", Color.RED, "X");
        berrySprite = loadEntitySprite("items/berry.png", Color.MAGENTA, "B");
        seedSprite = loadEntitySprite("items/energy_seed.png", Color.GREEN, "E");
        tripwireSprite = createPlaceholder(new Color(255, 100, 100, 150), "!");
    }
    
    private void loadTileSprite(int id, String filename) {
        try {
            BufferedImage img = ImageIO.read(new File("res/sprites/tiles/" + filename));
            tileSprites.put(id, img);
        } catch (IOException e) {
            // Fallback colors will be used
        }
    }
    
    private BufferedImage loadEntitySprite(String path, Color fallbackColor, String label) {
        try {
            return ImageIO.read(new File("res/sprites/" + path));
        } catch (IOException e) {
            return createPlaceholder(fallbackColor, label);
        }
    }
    
    private BufferedImage createPlaceholder(Color color, String label) {
        BufferedImage img = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, TILE_SIZE, TILE_SIZE);
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 24));
        FontMetrics fm = g.getFontMetrics();
        int x = (TILE_SIZE - fm.stringWidth(label)) / 2;
        int y = (TILE_SIZE + fm.getAscent()) / 2 - 4;
        g.drawString(label, x, y);
        g.dispose();
        return img;
    }
    
    // ========== UI BUILDING ==========
    
    private void buildUI() {
        setLayout(new BorderLayout());
        
        // Main canvas in center
        mapCanvas = new MapCanvas();
        JScrollPane canvasScroll = new JScrollPane(mapCanvas);
        canvasScroll.getVerticalScrollBar().setUnitIncrement(16);
        canvasScroll.getHorizontalScrollBar().setUnitIncrement(16);
        
        // Left panel: Tile palette
        tilePalette = new TilePalette();
        JScrollPane paletteScroll = new JScrollPane(tilePalette);
        paletteScroll.setPreferredSize(new Dimension(200, 0));
        paletteScroll.setBorder(new TitledBorder("Tile Palette"));
        
        // Right panel: Entity panel + Properties
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setPreferredSize(new Dimension(250, 0));
        
        entityPanel = new EntityPanel();
        entityPanel.setBorder(new TitledBorder("Entities"));
        
        propertiesPanel = new PropertiesPanel();
        propertiesPanel.setBorder(new TitledBorder("Properties"));
        propertiesPanel.setPreferredSize(new Dimension(0, 200));
        
        rightPanel.add(entityPanel, BorderLayout.CENTER);
        rightPanel.add(propertiesPanel, BorderLayout.SOUTH);
        
        // Bottom panel: Validation + Tools
        JPanel bottomPanel = new JPanel(new BorderLayout());
        
        validationPanel = new ValidationPanel();
        validationPanel.setPreferredSize(new Dimension(0, 150));
        validationPanel.setBorder(new TitledBorder("Validation"));
        
        JPanel toolsPanel = createToolsPanel();
        toolsPanel.setBorder(new TitledBorder("Tools"));
        
        bottomPanel.add(validationPanel, BorderLayout.CENTER);
        bottomPanel.add(toolsPanel, BorderLayout.EAST);
        
        // Status bar
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(new EmptyBorder(2, 5, 2, 5));
        statusLabel = new JLabel("Ready");
        coordinateLabel = new JLabel("Tile: (0, 0)  Pixel: (0, 0)");
        statusBar.add(statusLabel, BorderLayout.WEST);
        statusBar.add(coordinateLabel, BorderLayout.EAST);
        
        // Assemble
        add(paletteScroll, BorderLayout.WEST);
        add(canvasScroll, BorderLayout.CENTER);
        add(rightPanel, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);
        add(statusBar, BorderLayout.NORTH);
    }
    
    private JPanel createToolsPanel() {
        JPanel panel = new JPanel(new GridLayout(0, 2, 5, 5));
        panel.setPreferredSize(new Dimension(200, 0));
        
        // Brush size
        JLabel sizeLabel = new JLabel("Brush Size:");
        JSpinner sizeSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 10, 1));
        sizeSpinner.addChangeListener(e -> tileTools.setBrushSize((Integer) sizeSpinner.getValue()));
        
        // Tool buttons
        JButton brushBtn = new JButton("Brush (B)");
        JButton eraseBtn = new JButton("Erase (E)");
        JButton rectBtn = new JButton("Rect (R)");
        JButton fillBtn = new JButton("Fill (F)");
        JButton lineBtn = new JButton("Line (L)");
        JButton pickBtn = new JButton("Pick (I)");
        
        brushBtn.addActionListener(e -> setTileTool(TileTools.ToolMode.BRUSH));
        eraseBtn.addActionListener(e -> setTileTool(TileTools.ToolMode.ERASER));
        rectBtn.addActionListener(e -> setTileTool(TileTools.ToolMode.RECTANGLE));
        fillBtn.addActionListener(e -> setTileTool(TileTools.ToolMode.FILL));
        lineBtn.addActionListener(e -> setTileTool(TileTools.ToolMode.LINE));
        pickBtn.addActionListener(e -> setTileTool(TileTools.ToolMode.EYEDROPPER));
        
        panel.add(sizeLabel);
        panel.add(sizeSpinner);
        panel.add(brushBtn);
        panel.add(eraseBtn);
        panel.add(rectBtn);
        panel.add(fillBtn);
        panel.add(lineBtn);
        panel.add(pickBtn);
        
        return panel;
    }
    
    private void buildMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        
        // File menu
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);
        
        JMenuItem newItem = new JMenuItem("New Level", KeyEvent.VK_N);
        newItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK));
        newItem.addActionListener(e -> newLevel());
        
        JMenuItem openItem = new JMenuItem("Open...", KeyEvent.VK_O);
        openItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
        openItem.addActionListener(e -> openLevel());
        
        JMenuItem importItem = new JMenuItem("Import Text Map...", KeyEvent.VK_I);
        importItem.addActionListener(e -> importTextMap());
        
        JMenuItem saveItem = new JMenuItem("Save", KeyEvent.VK_S);
        saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
        saveItem.addActionListener(e -> saveLevel());
        
        JMenuItem saveAsItem = new JMenuItem("Save As...", KeyEvent.VK_A);
        saveAsItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, 
            InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
        saveAsItem.addActionListener(e -> saveLevelAs());
        
        JMenuItem exportMapItem = new JMenuItem("Export Text Map...");
        exportMapItem.addActionListener(e -> exportTextMap());
        
        JMenuItem exportJavaItem = new JMenuItem("Export Java Config...");
        exportJavaItem.addActionListener(e -> exportJavaConfig());
        
        JMenuItem exportPngItem = new JMenuItem("Export PNG...");
        exportPngItem.addActionListener(e -> exportPNG());
        
        JMenuItem exitItem = new JMenuItem("Exit", KeyEvent.VK_X);
        exitItem.addActionListener(e -> handleExit());
        
        fileMenu.add(newItem);
        fileMenu.add(openItem);
        fileMenu.add(importItem);
        fileMenu.addSeparator();
        fileMenu.add(saveItem);
        fileMenu.add(saveAsItem);
        fileMenu.addSeparator();
        fileMenu.add(exportMapItem);
        fileMenu.add(exportJavaItem);
        fileMenu.add(exportPngItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);
        
        // Edit menu
        JMenu editMenu = new JMenu("Edit");
        editMenu.setMnemonic(KeyEvent.VK_E);
        
        JMenuItem undoItem = new JMenuItem("Undo", KeyEvent.VK_U);
        undoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK));
        undoItem.addActionListener(e -> undo());
        
        JMenuItem redoItem = new JMenuItem("Redo", KeyEvent.VK_R);
        redoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK));
        redoItem.addActionListener(e -> redo());
        
        JMenuItem resizeItem = new JMenuItem("Resize Level...");
        resizeItem.addActionListener(e -> resizeLevel());
        
        editMenu.add(undoItem);
        editMenu.add(redoItem);
        editMenu.addSeparator();
        editMenu.add(resizeItem);
        
        // View menu
        JMenu viewMenu = new JMenu("View");
        viewMenu.setMnemonic(KeyEvent.VK_V);
        
        JMenuItem zoomInItem = new JMenuItem("Zoom In");
        zoomInItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, InputEvent.CTRL_DOWN_MASK));
        zoomInItem.addActionListener(e -> zoom(1.25f));
        
        JMenuItem zoomOutItem = new JMenuItem("Zoom Out");
        zoomOutItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, InputEvent.CTRL_DOWN_MASK));
        zoomOutItem.addActionListener(e -> zoom(0.8f));
        
        JMenuItem zoomResetItem = new JMenuItem("Reset Zoom");
        zoomResetItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_0, InputEvent.CTRL_DOWN_MASK));
        zoomResetItem.addActionListener(e -> { zoomLevel = 1.0f; mapCanvas.repaint(); });
        
        JCheckBoxMenuItem gridItem = new JCheckBoxMenuItem("Show Grid", true);
        gridItem.addActionListener(e -> mapCanvas.setShowGrid(gridItem.isSelected()));
        
        JCheckBoxMenuItem entitiesItem = new JCheckBoxMenuItem("Show Entities", true);
        entitiesItem.addActionListener(e -> mapCanvas.setShowEntities(entitiesItem.isSelected()));
        
        viewMenu.add(zoomInItem);
        viewMenu.add(zoomOutItem);
        viewMenu.add(zoomResetItem);
        viewMenu.addSeparator();
        viewMenu.add(gridItem);
        viewMenu.add(entitiesItem);
        
        // Mode menu
        JMenu modeMenu = new JMenu("Mode");
        
        ButtonGroup modeGroup = new ButtonGroup();
        JRadioButtonMenuItem tilesMode = new JRadioButtonMenuItem("Tile Editing", true);
        JRadioButtonMenuItem entitiesMode = new JRadioButtonMenuItem("Entity Editing");
        
        tilesMode.addActionListener(e -> setEditMode(EditMode.TILES));
        entitiesMode.addActionListener(e -> setEditMode(EditMode.ENTITIES));
        
        modeGroup.add(tilesMode);
        modeGroup.add(entitiesMode);
        modeMenu.add(tilesMode);
        modeMenu.add(entitiesMode);
        
        // Validate menu
        JMenu validateMenu = new JMenu("Validate");
        
        JMenuItem validateItem = new JMenuItem("Validate Level");
        validateItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
        validateItem.addActionListener(e -> validateLevel());
        
        validateMenu.add(validateItem);
        
        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(viewMenu);
        menuBar.add(modeMenu);
        menuBar.add(validateMenu);
        
        setJMenuBar(menuBar);
    }
    
    private void setupKeyBindings() {
        InputMap im = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getRootPane().getActionMap();
        
        // Tool shortcuts
        im.put(KeyStroke.getKeyStroke('b'), "brush");
        im.put(KeyStroke.getKeyStroke('e'), "eraser");
        im.put(KeyStroke.getKeyStroke('r'), "rect");
        im.put(KeyStroke.getKeyStroke('f'), "fill");
        im.put(KeyStroke.getKeyStroke('l'), "line");
        im.put(KeyStroke.getKeyStroke('i'), "pick");
        
        am.put("brush", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { setTileTool(TileTools.ToolMode.BRUSH); }
        });
        am.put("eraser", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { setTileTool(TileTools.ToolMode.ERASER); }
        });
        am.put("rect", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { setTileTool(TileTools.ToolMode.RECTANGLE); }
        });
        am.put("fill", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { setTileTool(TileTools.ToolMode.FILL); }
        });
        am.put("line", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { setTileTool(TileTools.ToolMode.LINE); }
        });
        am.put("pick", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { setTileTool(TileTools.ToolMode.EYEDROPPER); }
        });
        
        // Delete key
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete");
        am.put("delete", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { deleteSelected(); }
        });
        
        // Escape to cancel
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel");
        am.put("cancel", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                entityTools.cancelSpider();
                entityTools.clearSelection();
                mapCanvas.repaint();
            }
        });
        
        // Enter to finish spider
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "finish");
        am.put("finish", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                UndoManager.EditAction action = entityTools.finishSpider(levelData);
                if (action != null) {
                    undoManager.execute(action, levelData);
                    markModified();
                    mapCanvas.repaint();
                }
            }
        });
    }
    
    // ========== TOOL OPERATIONS ==========
    
    private void setTileTool(TileTools.ToolMode mode) {
        tileTools.setMode(mode);
        setEditMode(EditMode.TILES);
        statusLabel.setText("Tool: " + mode.name);
    }
    
    private void setEditMode(EditMode mode) {
        this.editMode = mode;
        statusLabel.setText("Mode: " + (mode == EditMode.TILES ? "Tile Editing" : "Entity Editing"));
    }
    
    private void undo() {
        if (undoManager.canUndo()) {
            undoManager.undo(levelData);
            markModified();
            mapCanvas.repaint();
            statusLabel.setText("Undo: " + undoManager.getUndoDescription());
        }
    }
    
    private void redo() {
        if (undoManager.canRedo()) {
            undoManager.redo(levelData);
            markModified();
            mapCanvas.repaint();
            statusLabel.setText("Redo: " + undoManager.getRedoDescription());
        }
    }
    
    private void deleteSelected() {
        if (editMode == EditMode.ENTITIES) {
            UndoManager.EditAction action = entityTools.deleteSelected(levelData);
            if (action != null) {
                undoManager.execute(action, levelData);
                markModified();
                mapCanvas.repaint();
            }
        }
    }
    
    private void zoom(float factor) {
        zoomLevel = Math.max(0.25f, Math.min(4.0f, zoomLevel * factor));
        mapCanvas.revalidate();
        mapCanvas.repaint();
    }
    
    // ========== FILE OPERATIONS ==========
    
    private void newLevel() {
        if (!confirmDiscard()) return;
        
        JTextField nameField = new JTextField("New Level");
        JSpinner widthSpinner = new JSpinner(new SpinnerNumberModel(30, 10, 100, 1));
        JSpinner heightSpinner = new JSpinner(new SpinnerNumberModel(20, 10, 100, 1));
        
        JPanel panel = new JPanel(new GridLayout(0, 2, 5, 5));
        panel.add(new JLabel("Level Name:"));
        panel.add(nameField);
        panel.add(new JLabel("Width (tiles):"));
        panel.add(widthSpinner);
        panel.add(new JLabel("Height (tiles):"));
        panel.add(heightSpinner);
        
        int result = JOptionPane.showConfirmDialog(this, panel, "New Level", 
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        
        if (result == JOptionPane.OK_OPTION) {
            levelData = new LevelData(nameField.getText(), 
                (Integer) widthSpinner.getValue(), 
                (Integer) heightSpinner.getValue());
            currentFilePath = null;
            modified = false;
            undoManager.clear();
            mapCanvas.repaint();
            updateTitle();
        }
    }
    
    private void openLevel() {
        if (!confirmDiscard()) return;
        
        JFileChooser chooser = new JFileChooser("res/maps");
        chooser.setFileFilter(new FileNameExtensionFilter("Level Files (*.json)", "json"));
        
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                levelData = LevelDataIO.loadFromJson(chooser.getSelectedFile().toPath());
                currentFilePath = chooser.getSelectedFile().getAbsolutePath();
                modified = false;
                undoManager.clear();
                mapCanvas.repaint();
                updateTitle();
                validateLevel();
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Failed to load level: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void importTextMap() {
        JFileChooser chooser = new JFileChooser("res/maps");
        chooser.setFileFilter(new FileNameExtensionFilter("Text Map Files (*.txt)", "txt"));
        
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            String name = chooser.getSelectedFile().getName().replace(".txt", "");
            name = JOptionPane.showInputDialog(this, "Enter level name:", name);
            if (name == null) return;
            
            try {
                levelData = LevelDataIO.importFromTextMap(chooser.getSelectedFile().toPath(), name);
                currentFilePath = null;
                modified = true;
                undoManager.clear();
                mapCanvas.repaint();
                updateTitle();
                statusLabel.setText("Imported text map - entities need to be placed manually");
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Failed to import: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void saveLevel() {
        if (currentFilePath == null) {
            saveLevelAs();
        } else {
            try {
                LevelDataIO.saveToJson(levelData, Path.of(currentFilePath));
                modified = false;
                updateTitle();
                statusLabel.setText("Saved to " + currentFilePath);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Failed to save: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void saveLevelAs() {
        JFileChooser chooser = new JFileChooser("res/maps");
        chooser.setFileFilter(new FileNameExtensionFilter("Level Files (*.json)", "json"));
        chooser.setSelectedFile(new File(levelData.getName().toLowerCase().replace(" ", "_") + ".json"));
        
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            if (!file.getName().endsWith(".json")) {
                file = new File(file.getPath() + ".json");
            }
            
            try {
                LevelDataIO.saveToJson(levelData, file.toPath());
                currentFilePath = file.getAbsolutePath();
                modified = false;
                updateTitle();
                statusLabel.setText("Saved to " + currentFilePath);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Failed to save: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void exportTextMap() {
        JFileChooser chooser = new JFileChooser("res/maps");
        chooser.setFileFilter(new FileNameExtensionFilter("Text Map Files (*.txt)", "txt"));
        chooser.setSelectedFile(new File(levelData.getName().toLowerCase().replace(" ", "_") + ".txt"));
        
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            if (!file.getName().endsWith(".txt")) {
                file = new File(file.getPath() + ".txt");
            }
            
            try {
                LevelDataIO.exportToTextMap(levelData, file.toPath());
                statusLabel.setText("Exported text map to " + file.getName());
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Failed to export: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void exportJavaConfig() {
        JFileChooser chooser = new JFileChooser("src/com/buglife/levels");
        chooser.setFileFilter(new FileNameExtensionFilter("Java Files (*.java)", "java"));
        
        String className = levelData.getName().replaceAll("[^a-zA-Z0-9]", "") + "Config";
        chooser.setSelectedFile(new File(className + ".java"));
        
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            if (!file.getName().endsWith(".java")) {
                file = new File(file.getPath() + ".java");
            }
            
            try {
                String javaCode = LevelDataIO.generateJavaConfig(levelData, className);
                Files.writeString(file.toPath(), javaCode);
                statusLabel.setText("Exported Java config to " + file.getName());
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Failed to export: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void exportPNG() {
        JFileChooser chooser = new JFileChooser(".");
        chooser.setFileFilter(new FileNameExtensionFilter("PNG Images (*.png)", "png"));
        chooser.setSelectedFile(new File(levelData.getName().toLowerCase().replace(" ", "_") + ".png"));
        
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            if (!file.getName().endsWith(".png")) {
                file = new File(file.getPath() + ".png");
            }
            
            try {
                BufferedImage img = mapCanvas.renderToImage();
                ImageIO.write(img, "PNG", file);
                statusLabel.setText("Exported PNG to " + file.getName());
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Failed to export: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void resizeLevel() {
        JSpinner widthSpinner = new JSpinner(new SpinnerNumberModel(levelData.getWidth(), 10, 100, 1));
        JSpinner heightSpinner = new JSpinner(new SpinnerNumberModel(levelData.getHeight(), 10, 100, 1));
        
        JPanel panel = new JPanel(new GridLayout(0, 2, 5, 5));
        panel.add(new JLabel("New Width:"));
        panel.add(widthSpinner);
        panel.add(new JLabel("New Height:"));
        panel.add(heightSpinner);
        
        int result = JOptionPane.showConfirmDialog(this, panel, "Resize Level",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        
        if (result == JOptionPane.OK_OPTION) {
            int newWidth = (Integer) widthSpinner.getValue();
            int newHeight = (Integer) heightSpinner.getValue();
            
            if (newWidth != levelData.getWidth() || newHeight != levelData.getHeight()) {
                levelData.resize(newWidth, newHeight);
                markModified();
                mapCanvas.repaint();
            }
        }
    }
    
    private void validateLevel() {
        List<ValidationIssue> issues = LevelValidator.validate(levelData);
        validationPanel.setIssues(issues);
    }
    
    private void handleExit() {
        if (confirmDiscard()) {
            dispose();
            System.exit(0);
        }
    }
    
    private boolean confirmDiscard() {
        if (!modified) return true;
        
        int result = JOptionPane.showConfirmDialog(this,
            "You have unsaved changes. Save before closing?",
            "Unsaved Changes",
            JOptionPane.YES_NO_CANCEL_OPTION,
            JOptionPane.WARNING_MESSAGE);
        
        if (result == JOptionPane.YES_OPTION) {
            saveLevel();
            return !modified;
        } else if (result == JOptionPane.NO_OPTION) {
            return true;
        }
        return false;
    }
    
    private void markModified() {
        modified = true;
        updateTitle();
    }
    
    private void updateTitle() {
        String title = "Lullaby Down Below - Level Editor";
        if (currentFilePath != null) {
            title += " - " + new File(currentFilePath).getName();
        } else {
            title += " - " + levelData.getName();
        }
        if (modified) {
            title += " *";
        }
        setTitle(title);
    }
    
    // ========== INNER CLASSES ==========
    
    /**
     * Main canvas for rendering and editing the map.
     */
    private class MapCanvas extends JPanel implements MouseListener, MouseMotionListener, MouseWheelListener {
        private boolean showGrid = true;
        private boolean showEntities = true;
        private Point dragStart = null;
        private Point dragCurrent = null;
        private boolean isDragging = false;
        
        public MapCanvas() {
            setBackground(new Color(40, 40, 50));
            addMouseListener(this);
            addMouseMotionListener(this);
            addMouseWheelListener(this);
        }
        
        public void setShowGrid(boolean show) { this.showGrid = show; repaint(); }
        public void setShowEntities(boolean show) { this.showEntities = show; repaint(); }
        
        @Override
        public Dimension getPreferredSize() {
            int w = (int) (levelData.getWidth() * TILE_SIZE * zoomLevel);
            int h = (int) (levelData.getHeight() * TILE_SIZE * zoomLevel);
            return new Dimension(w, h);
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            
            g2.scale(zoomLevel, zoomLevel);
            
            // Draw tiles
            for (int y = 0; y < levelData.getHeight(); y++) {
                for (int x = 0; x < levelData.getWidth(); x++) {
                    int tileId = levelData.getTile(x, y);
                    int px = x * TILE_SIZE;
                    int py = y * TILE_SIZE;
                    
                    BufferedImage sprite = tileSprites.get(tileId);
                    if (sprite != null) {
                        g2.drawImage(sprite, px, py, TILE_SIZE, TILE_SIZE, null);
                    } else {
                        g2.setColor(getTileColor(tileId));
                        g2.fillRect(px, py, TILE_SIZE, TILE_SIZE);
                    }
                }
            }
            
            // Draw grid
            if (showGrid) {
                g2.setColor(new Color(100, 100, 100, 80));
                for (int x = 0; x <= levelData.getWidth(); x++) {
                    g2.drawLine(x * TILE_SIZE, 0, x * TILE_SIZE, levelData.getHeight() * TILE_SIZE);
                }
                for (int y = 0; y <= levelData.getHeight(); y++) {
                    g2.drawLine(0, y * TILE_SIZE, levelData.getWidth() * TILE_SIZE, y * TILE_SIZE);
                }
            }
            
            // Draw entities
            if (showEntities) {
                drawEntities(g2);
            }
            
            // Draw pending spider path
            SpiderData pending = entityTools.getPendingSpider();
            if (pending != null && !pending.waypoints.isEmpty()) {
                g2.setColor(Color.ORANGE);
                g2.setStroke(new BasicStroke(3));
                List<PointData> waypoints = pending.waypoints;
                for (int i = 0; i < waypoints.size() - 1; i++) {
                    PointData a = waypoints.get(i);
                    PointData b = waypoints.get(i + 1);
                    g2.drawLine(a.x * TILE_SIZE + TILE_SIZE / 2, a.y * TILE_SIZE + TILE_SIZE / 2,
                               b.x * TILE_SIZE + TILE_SIZE / 2, b.y * TILE_SIZE + TILE_SIZE / 2);
                }
                for (PointData wp : waypoints) {
                    g2.fillOval(wp.x * TILE_SIZE + TILE_SIZE / 2 - 6, 
                               wp.y * TILE_SIZE + TILE_SIZE / 2 - 6, 12, 12);
                }
            }
            
            // Draw drag preview (for rectangle/line tools)
            if (dragStart != null && dragCurrent != null && editMode == EditMode.TILES) {
                g2.setColor(new Color(255, 255, 0, 100));
                TileTools.ToolMode mode = tileTools.getMode();
                if (mode == TileTools.ToolMode.RECTANGLE) {
                    int x1 = Math.min(dragStart.x, dragCurrent.x);
                    int y1 = Math.min(dragStart.y, dragCurrent.y);
                    int x2 = Math.max(dragStart.x, dragCurrent.x);
                    int y2 = Math.max(dragStart.y, dragCurrent.y);
                    for (int ty = y1; ty <= y2; ty++) {
                        for (int tx = x1; tx <= x2; tx++) {
                            g2.fillRect(tx * TILE_SIZE, ty * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                        }
                    }
                } else if (mode == TileTools.ToolMode.LINE) {
                    List<Point> line = tileTools.linePreview(dragStart.x, dragStart.y, 
                        dragCurrent.x, dragCurrent.y);
                    for (Point p : line) {
                        g2.fillRect(p.x * TILE_SIZE, p.y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                    }
                }
            }
            
            // Highlight selected entity
            Object selected = entityTools.getSelectedEntity();
            if (selected != null && showEntities) {
                g2.setColor(Color.YELLOW);
                g2.setStroke(new BasicStroke(3));
                String type = entityTools.getSelectedEntityType();
                if (selected instanceof PointData) {
                    PointData p = (PointData) selected;
                    if (type.equals("spider")) {
                        g2.drawOval(p.x * TILE_SIZE + TILE_SIZE/2 - 10, 
                                   p.y * TILE_SIZE + TILE_SIZE/2 - 10, 20, 20);
                    } else {
                        g2.drawOval(p.x - 20, p.y - 20, 40, 40);
                    }
                } else if (selected instanceof SnailData) {
                    SnailData s = (SnailData) selected;
                    g2.drawOval(s.position.x - 20, s.position.y - 20, 40, 40);
                } else if (selected instanceof FoodData) {
                    FoodData f = (FoodData) selected;
                    g2.drawOval(f.position.x * TILE_SIZE + TILE_SIZE/2 - 20, 
                               f.position.y * TILE_SIZE + TILE_SIZE/2 - 20, 40, 40);
                } else if (selected instanceof SpiderData) {
                    SpiderData s = (SpiderData) selected;
                    g2.drawRect(s.waypoints.get(0).x * TILE_SIZE - 5,
                               s.waypoints.get(0).y * TILE_SIZE - 5,
                               TILE_SIZE + 10, TILE_SIZE + 10);
                }
            }
        }
        
        private void drawEntities(Graphics2D g2) {
            // Draw spider paths and waypoints
            g2.setStroke(new BasicStroke(2));
            for (SpiderData spider : levelData.getSpiders()) {
                g2.setColor(new Color(255, 50, 50, 150));
                List<PointData> waypoints = spider.waypoints;
                for (int i = 0; i < waypoints.size() - 1; i++) {
                    PointData a = waypoints.get(i);
                    PointData b = waypoints.get(i + 1);
                    g2.drawLine(a.x * TILE_SIZE + TILE_SIZE / 2, a.y * TILE_SIZE + TILE_SIZE / 2,
                               b.x * TILE_SIZE + TILE_SIZE / 2, b.y * TILE_SIZE + TILE_SIZE / 2);
                }
                // Draw spider at first waypoint
                if (!waypoints.isEmpty()) {
                    PointData first = waypoints.get(0);
                    g2.drawImage(spiderSprite, first.x * TILE_SIZE, first.y * TILE_SIZE, 
                        TILE_SIZE, TILE_SIZE, null);
                    // Draw waypoint markers
                    for (PointData wp : waypoints) {
                        g2.setColor(Color.RED);
                        g2.fillOval(wp.x * TILE_SIZE + TILE_SIZE/2 - 5, 
                                   wp.y * TILE_SIZE + TILE_SIZE/2 - 5, 10, 10);
                    }
                }
            }
            
            // Draw snails
            for (SnailData snail : levelData.getSnails()) {
                g2.drawImage(snailSprite, snail.position.x - TILE_SIZE/2, 
                    snail.position.y - TILE_SIZE/2, TILE_SIZE, TILE_SIZE, null);
            }
            
            // Draw food
            for (FoodData food : levelData.getFood()) {
                BufferedImage sprite = food.type.equals("BERRY") ? berrySprite : seedSprite;
                g2.drawImage(sprite, food.position.x * TILE_SIZE, food.position.y * TILE_SIZE,
                    TILE_SIZE, TILE_SIZE, null);
            }
            
            // Draw tripwires
            g2.setColor(new Color(255, 100, 100));
            g2.setStroke(new BasicStroke(3));
            for (PointData tw : levelData.getTripwires()) {
                g2.drawLine(tw.x - 20, tw.y, tw.x + 20, tw.y);
                g2.drawLine(tw.x, tw.y - 5, tw.x, tw.y + 5);
            }
            
            // Draw player spawn
            if (levelData.getPlayerSpawn() != null) {
                PointData p = levelData.getPlayerSpawn();
                g2.drawImage(playerSprite, p.x - TILE_SIZE/2, p.y - TILE_SIZE/2, 
                    TILE_SIZE, TILE_SIZE, null);
                g2.setColor(Color.CYAN);
                g2.setStroke(new BasicStroke(2));
                g2.drawOval(p.x - TILE_SIZE/2 - 5, p.y - TILE_SIZE/2 - 5, 
                    TILE_SIZE + 10, TILE_SIZE + 10);
            }
            
            // Draw toy spawn
            if (levelData.getToySpawn() != null) {
                PointData p = levelData.getToySpawn();
                g2.drawImage(toySprite, p.x - TILE_SIZE/2, p.y - TILE_SIZE/2, 
                    TILE_SIZE, TILE_SIZE, null);
                g2.setColor(Color.YELLOW);
                g2.setStroke(new BasicStroke(2));
                g2.drawOval(p.x - TILE_SIZE/2 - 5, p.y - TILE_SIZE/2 - 5, 
                    TILE_SIZE + 10, TILE_SIZE + 10);
            }
        }
        
        private Color getTileColor(int id) {
            switch (id) {
                case TileConstants.FLOOR: return new Color(135, 206, 235, 50); // Light blue (air/walkable)
                case TileConstants.WALL: return new Color(139, 90, 43);        // Brown (solid)
                case TileConstants.WALL_ALT: return new Color(101, 67, 33);    // Dark brown
                case TileConstants.STICKY_FLOOR: return new Color(150, 120, 90); // Tan (slowdown)
                case TileConstants.BROKEN_TILE: return new Color(100, 80, 60);
                case TileConstants.SHADOW_TILE: return new Color(40, 40, 40);
                case TileConstants.LADDER_3: return new Color(255, 215, 0);    // Gold (exit)
                default: 
                    if (id >= TileConstants.STAIN_1 && id <= TileConstants.STAIN_4) {
                        return new Color(120, 100, 80);
                    }
                    return new Color(100, 70, 40);
            }
        }
        
        public BufferedImage renderToImage() {
            int w = levelData.getWidth() * TILE_SIZE;
            int h = levelData.getHeight() * TILE_SIZE;
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            float oldZoom = zoomLevel;
            zoomLevel = 1.0f;
            paintComponent(g);
            zoomLevel = oldZoom;
            g.dispose();
            return img;
        }
        
        // Mouse handling
        @Override
        public void mousePressed(MouseEvent e) {
            int tileX = (int) (e.getX() / (TILE_SIZE * zoomLevel));
            int tileY = (int) (e.getY() / (TILE_SIZE * zoomLevel));
            int pixelX = (int) (e.getX() / zoomLevel);
            int pixelY = (int) (e.getY() / zoomLevel);
            
            if (tileX < 0 || tileX >= levelData.getWidth() || 
                tileY < 0 || tileY >= levelData.getHeight()) return;
            
            if (editMode == EditMode.TILES) {
                TileTools.ToolMode mode = tileTools.getMode();
                if (mode == TileTools.ToolMode.RECTANGLE || mode == TileTools.ToolMode.LINE) {
                    dragStart = new Point(tileX, tileY);
                    dragCurrent = new Point(tileX, tileY);
                } else if (mode == TileTools.ToolMode.EYEDROPPER) {
                    int picked = tileTools.eyedropper(levelData, tileX, tileY);
                    tilePalette.selectTile(picked);
                } else if (mode == TileTools.ToolMode.FILL) {
                    UndoManager.EditAction action = tileTools.fill(levelData, tileX, tileY, 
                        tilePalette.getSelectedTile());
                    if (action != null) {
                        undoManager.execute(action, levelData);
                        markModified();
                    }
                } else {
                    // Brush/Eraser
                    int tileId = mode == TileTools.ToolMode.ERASER ? 
                        TileConstants.FLOOR : tilePalette.getSelectedTile();
                    UndoManager.EditAction action = tileTools.brush(levelData, tileX, tileY, tileId);
                    if (action != null) {
                        undoManager.execute(action, levelData);
                        markModified();
                    }
                    isDragging = true;
                }
            } else {
                // Entity mode
                UndoManager.EditAction action = entityTools.handleClick(levelData, tileX, tileY, 
                    pixelX, pixelY, undoManager);
                if (action != null) {
                    undoManager.execute(action, levelData);
                    markModified();
                }
            }
            
            repaint();
        }
        
        @Override
        public void mouseDragged(MouseEvent e) {
            int tileX = (int) (e.getX() / (TILE_SIZE * zoomLevel));
            int tileY = (int) (e.getY() / (TILE_SIZE * zoomLevel));
            
            if (tileX < 0 || tileX >= levelData.getWidth() || 
                tileY < 0 || tileY >= levelData.getHeight()) return;
            
            if (editMode == EditMode.TILES) {
                TileTools.ToolMode mode = tileTools.getMode();
                if (mode == TileTools.ToolMode.RECTANGLE || mode == TileTools.ToolMode.LINE) {
                    dragCurrent = new Point(tileX, tileY);
                    repaint();
                } else if (isDragging && (mode == TileTools.ToolMode.BRUSH || 
                           mode == TileTools.ToolMode.ERASER)) {
                    int tileId = mode == TileTools.ToolMode.ERASER ? 
                        TileConstants.FLOOR : tilePalette.getSelectedTile();
                    UndoManager.EditAction action = tileTools.brush(levelData, tileX, tileY, tileId);
                    if (action != null) {
                        undoManager.execute(action, levelData);
                        markModified();
                        repaint();
                    }
                }
            }
            
            updateCoordinates(e);
        }
        
        @Override
        public void mouseReleased(MouseEvent e) {
            int tileX = (int) (e.getX() / (TILE_SIZE * zoomLevel));
            int tileY = (int) (e.getY() / (TILE_SIZE * zoomLevel));
            
            if (editMode == EditMode.TILES && dragStart != null) {
                TileTools.ToolMode mode = tileTools.getMode();
                int tileId = tilePalette.getSelectedTile();
                
                UndoManager.EditAction action = null;
                if (mode == TileTools.ToolMode.RECTANGLE) {
                    action = tileTools.rectangle(levelData, dragStart.x, dragStart.y, 
                        tileX, tileY, tileId);
                } else if (mode == TileTools.ToolMode.LINE) {
                    action = tileTools.line(levelData, dragStart.x, dragStart.y, 
                        tileX, tileY, tileId);
                }
                
                if (action != null) {
                    undoManager.execute(action, levelData);
                    markModified();
                }
            }
            
            dragStart = null;
            dragCurrent = null;
            isDragging = false;
            repaint();
        }
        
        @Override
        public void mouseMoved(MouseEvent e) {
            updateCoordinates(e);
        }
        
        private void updateCoordinates(MouseEvent e) {
            int tileX = (int) (e.getX() / (TILE_SIZE * zoomLevel));
            int tileY = (int) (e.getY() / (TILE_SIZE * zoomLevel));
            int pixelX = (int) (e.getX() / zoomLevel);
            int pixelY = (int) (e.getY() / zoomLevel);
            coordinateLabel.setText(String.format("Tile: (%d, %d)  Pixel: (%d, %d)  Zoom: %.0f%%",
                tileX, tileY, pixelX, pixelY, zoomLevel * 100));
        }
        
        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            if (e.isControlDown()) {
                float factor = e.getWheelRotation() < 0 ? 1.1f : 0.9f;
                zoom(factor);
            }
        }
        
        @Override public void mouseClicked(MouseEvent e) {}
        @Override public void mouseEntered(MouseEvent e) {}
        @Override public void mouseExited(MouseEvent e) {}
    }
    
    /**
     * Tile palette for selecting tiles to paint.
     */
    private class TilePalette extends JPanel {
        private int selectedTile = TileConstants.WALL;
        
        // Use actual tile IDs from TileConstants
        private final int[] TILE_IDS = {
            TileConstants.FLOOR,        // 0 - Empty/walkable
            TileConstants.WALL,         // 1 - Solid wall
            TileConstants.WALL_ALT,     // 2 - Wall variant
            TileConstants.STICKY_FLOOR, // 3 - Slowdown
            TileConstants.BROKEN_TILE,  // 4 - Broken
            TileConstants.SHADOW_TILE,  // 5 - Shadow/dark
            TileConstants.STAIN_1,      // 6
            TileConstants.STAIN_2,      // 7
            TileConstants.STAIN_3,      // 8
            TileConstants.STAIN_4,      // 9
            TileConstants.SACK_W1,      // 11
            TileConstants.SACK_W2,      // 12
            TileConstants.SACK_W3,      // 13
            TileConstants.SACK_W4,      // 14
            TileConstants.PLANK_1,      // 31
            TileConstants.PLANK_2,      // 32
            TileConstants.PLANK_3,      // 33
            TileConstants.PLANK_4,      // 34
            TileConstants.LADDER_1,     // 35
            TileConstants.LADDER_2,     // 36
            TileConstants.LADDER_3,     // 37 - Exit!
            TileConstants.LADDER_4,     // 38
        };
        
        public TilePalette() {
            setLayout(new GridLayout(0, 4, 4, 4));
            setBorder(new EmptyBorder(5, 5, 5, 5));
            
            for (int id : TILE_IDS) {
                JButton btn = createTileButton(id);
                add(btn);
            }
        }
        
        private JButton createTileButton(int tileId) {
            JButton btn = new JButton() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    BufferedImage sprite = tileSprites.get(tileId);
                    if (sprite != null) {
                        g.drawImage(sprite, 2, 2, getWidth() - 4, getHeight() - 4, null);
                    } else {
                        g.setColor(mapCanvas.getTileColor(tileId));
                        g.fillRect(2, 2, getWidth() - 4, getHeight() - 4);
                    }
                    if (selectedTile == tileId) {
                        g.setColor(Color.YELLOW);
                        ((Graphics2D)g).setStroke(new BasicStroke(3));
                        g.drawRect(1, 1, getWidth() - 2, getHeight() - 2);
                    }
                }
            };
            btn.setPreferredSize(new Dimension(PALETTE_TILE_SIZE + 8, PALETTE_TILE_SIZE + 8));
            btn.setToolTipText(TileConstants.getTileName(tileId));
            btn.addActionListener(e -> {
                selectedTile = tileId;
                repaint();
                getParent().repaint();
            });
            return btn;
        }
        
        public int getSelectedTile() { return selectedTile; }
        
        public void selectTile(int id) {
            selectedTile = id;
            repaint();
        }
    }
    
    /**
     * Entity panel for entity mode selection.
     */
    private class EntityPanel extends JPanel {
        public EntityPanel() {
            setLayout(new GridLayout(0, 1, 5, 5));
            setBorder(new EmptyBorder(5, 5, 5, 5));
            
            ButtonGroup group = new ButtonGroup();
            
            for (EntityTools.EntityMode mode : EntityTools.EntityMode.values()) {
                JRadioButton btn = new JRadioButton(mode.name);
                btn.setToolTipText(mode.description);
                btn.addActionListener(e -> {
                    entityTools.setMode(mode);
                    setEditMode(EditMode.ENTITIES);
                });
                if (mode == EntityTools.EntityMode.SELECT) {
                    btn.setSelected(true);
                }
                group.add(btn);
                add(btn);
            }
            
            add(Box.createVerticalStrut(10));
            
            JButton finishSpiderBtn = new JButton("Finish Spider (Enter)");
            finishSpiderBtn.addActionListener(e -> {
                UndoManager.EditAction action = entityTools.finishSpider(levelData);
                if (action != null) {
                    undoManager.execute(action, levelData);
                    markModified();
                    mapCanvas.repaint();
                }
            });
            add(finishSpiderBtn);
            
            JButton deleteBtn = new JButton("Delete Selected (Del)");
            deleteBtn.addActionListener(e -> deleteSelected());
            add(deleteBtn);
        }
    }
    
    /**
     * Properties panel for editing selected entity properties.
     */
    private class PropertiesPanel extends JPanel {
        private JTextArea propertiesArea;
        
        public PropertiesPanel() {
            setLayout(new BorderLayout());
            propertiesArea = new JTextArea();
            propertiesArea.setEditable(false);
            propertiesArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
            add(new JScrollPane(propertiesArea), BorderLayout.CENTER);
        }
        
        public void updateProperties(Object entity, String type) {
            if (entity == null) {
                propertiesArea.setText("No entity selected");
                return;
            }
            
            StringBuilder sb = new StringBuilder();
            sb.append("Type: ").append(type).append("\n\n");
            
            if (entity instanceof PointData) {
                PointData p = (PointData) entity;
                sb.append("Position: (").append(p.x).append(", ").append(p.y).append(")\n");
            } else if (entity instanceof SnailData) {
                SnailData s = (SnailData) entity;
                sb.append("Position: (").append(s.position.x).append(", ").append(s.position.y).append(")\n");
                sb.append("Dialogue:\n");
                for (String line : s.dialogue) {
                    sb.append("  - ").append(line).append("\n");
                }
            } else if (entity instanceof SpiderData) {
                SpiderData s = (SpiderData) entity;
                sb.append("Waypoints: ").append(s.waypoints.size()).append("\n");
                for (int i = 0; i < s.waypoints.size(); i++) {
                    PointData wp = s.waypoints.get(i);
                    sb.append("  ").append(i + 1).append(": (").append(wp.x).append(", ").append(wp.y).append(")\n");
                }
            } else if (entity instanceof FoodData) {
                FoodData f = (FoodData) entity;
                sb.append("Type: ").append(f.type).append("\n");
                sb.append("Position: (").append(f.position.x).append(", ").append(f.position.y).append(")\n");
            }
            
            propertiesArea.setText(sb.toString());
        }
    }
    
    /**
     * Panel for displaying validation issues.
     */
    private class ValidationPanel extends JPanel {
        private DefaultListModel<String> listModel;
        private JList<String> issueList;
        private List<ValidationIssue> issues = new ArrayList<>();
        
        public ValidationPanel() {
            setLayout(new BorderLayout());
            
            listModel = new DefaultListModel<>();
            issueList = new JList<>(listModel);
            issueList.setCellRenderer(new IssueCellRenderer());
            issueList.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        int idx = issueList.getSelectedIndex();
                        if (idx >= 0 && idx < issues.size()) {
                            // Could jump to location if issue has coordinates
                            statusLabel.setText(issues.get(idx).message);
                        }
                    }
                }
            });
            
            add(new JScrollPane(issueList), BorderLayout.CENTER);
            
            JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JButton validateBtn = new JButton("Validate");
            validateBtn.addActionListener(e -> validateLevel());
            btnPanel.add(validateBtn);
            
            JLabel countLabel = new JLabel("");
            btnPanel.add(countLabel);
            
            add(btnPanel, BorderLayout.SOUTH);
        }
        
        public void setIssues(List<ValidationIssue> issues) {
            this.issues = issues;
            listModel.clear();
            
            int errors = 0, warnings = 0;
            for (ValidationIssue issue : issues) {
                String prefix = issue.severity == LevelValidator.Severity.ERROR ? "[ERROR] " : "[WARN] ";
                listModel.addElement(prefix + issue.message);
                if (issue.severity == LevelValidator.Severity.ERROR) errors++;
                else warnings++;
            }
            
            if (issues.isEmpty()) {
                listModel.addElement("✓ No issues found - level is valid!");
            }
            
            statusLabel.setText(String.format("Validation: %d errors, %d warnings", errors, warnings));
        }
        
        private class IssueCellRenderer extends DefaultListCellRenderer {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, 
                    int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                String text = value.toString();
                if (text.startsWith("[ERROR]")) {
                    setForeground(isSelected ? Color.WHITE : Color.RED);
                } else if (text.startsWith("[WARN]")) {
                    setForeground(isSelected ? Color.WHITE : new Color(200, 150, 0));
                } else if (text.startsWith("✓")) {
                    setForeground(isSelected ? Color.WHITE : new Color(0, 150, 0));
                }
                return this;
            }
        }
    }
    
    // ========== MAIN ==========
    
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Use default
        }
        
        SwingUtilities.invokeLater(LevelEditor::new);
    }
}
