# Level Editor Documentation

## Overview

The **Lullaby Down Below Level Editor** is a full-featured Unity/Godot-style tile map editor that allows you to create and edit levels for the game without running the entire game.

## Features

### Tile Editing

- **Brush Tool (B)** - Paint tiles with variable brush size
- **Eraser Tool (E)** - Erase tiles (set to AIR)
- **Rectangle Tool (R)** - Draw filled rectangles
- **Fill Tool (F)** - Flood fill an area
- **Line Tool (L)** - Draw lines using Bresenham's algorithm
- **Eyedropper Tool (I)** - Pick tile type from the canvas

### Entity Editing

- **Player Spawn** - Set where the player starts
- **Toy Spawn** - Set where the baby's toy is placed
- **Snails** - Add friendly NPC snails with dialogue
- **Spiders** - Create spider patrol paths with waypoints
- **Food (Berry/Energy Seed)** - Place collectible food items
- **Tripwires** - Add danger zones

### Persistence

- **JSON Format** - Native save/load with full entity data
- **Text Map Import** - Import legacy `levelX.txt` tile maps
- **Text Map Export** - Export tiles back to text format
- **Java Config Export** - Generate `LevelXConfig.java` code snippets

### Validation

- Detects missing player spawn points
- Validates exit tile existence
- Checks spider patrol paths for validity
- Warns about out-of-bounds entities
- Analyzes map quality (solid ratio, border walls)

### Undo/Redo
- Full 100-step undo history
- Support for tile operations, entity additions/deletions/moves
- Compound actions for multi-tile operations

## Keyboard Shortcuts

| Key | Action |
|-----|--------|
| B | Brush tool |
| E | Eraser tool |
| R | Rectangle tool |
| F | Fill tool |
| L | Line tool |
| I | Eyedropper (pick) tool |
| Ctrl+Z | Undo |
| Ctrl+Y | Redo |
| Ctrl+S | Save |
| Ctrl+O | Open |
| Ctrl+N | New level |
| Delete | Delete selected entity |
| Escape | Cancel current operation |
| Enter | Finish spider patrol path |
| Ctrl+Mouse Wheel | Zoom in/out |
| F5 | Validate level |

## File Locations

### Editor Source Files
```
src/com/buglife/engine/editor/
├── LevelEditor.java           # Main editor UI
├── data/
│   ├── LevelData.java         # Level data model
│   └── LevelDataIO.java       # Save/load operations
├── tools/
│   ├── TileTools.java         # Tile painting tools
│   ├── EntityTools.java       # Entity editing tools
│   └── UndoManager.java       # Undo/redo system
└── validation/
    └── LevelValidator.java    # Level validation
```

### Level Files
- **JSON Levels**: `res/maps/*.json` (new format with entities)
- **Text Maps**: `res/maps/*.txt` (legacy tile-only format)

## Running the Editor

### From Batch File
```bash
run-level-editor.bat
```

### From Maven
```bash
mvn compile exec:java -Peditor
```

### From IDE
Run `com.buglife.engine.editor.LevelEditor` main class.

## Workflow

### Creating a New Level
1. **File > New Level** - Set name and dimensions
2. Use **tile palette** on the left to select tiles
3. Use **brush/rectangle/fill** tools to paint the map
4. Switch to **Entity Editing** mode
5. Place **Player Spawn** (required)
6. Add **Exit Tile** in the tile map
7. Place enemies (spiders), NPCs (snails), and collectibles
8. **Validate** (F5) to check for issues
9. **Save** as JSON for full data preservation

### Editing an Existing Level
1. **File > Open** to load a `.json` level
2. Or **File > Import Text Map** for legacy `.txt` files
3. Make changes
4. Save

### Exporting for the Game
1. **Export Text Map** - For the tile data (`res/maps/levelX.txt`)
2. **Export Java Config** - For the entity configuration code

## Dev/Prod Separation

The editor and developer tools are **excluded from production builds**.

### Development Build (default)
```bash
mvn package
# Includes all tools and editor
```

### Production Build
```bash
mvn package -Pprod
# Excludes: com.buglife.tools.*, com.buglife.engine.editor.*, DebugExporter
```

### Excluded in Production
- `com/buglife/tools/**` - MapPreviewTool, etc.
- `com/buglife/engine/editor/**` - Full level editor
- `com/buglife/utils/DebugExporter.java` - Debug utilities
- `debug-settings.json` - Debug configuration

## Entity Data Format

### JSON Level Format
```json
{
  "name": "Level 1",
  "width": 30,
  "height": 20,
  "tiles": [[0,0,1,1,...], ...],
  "playerSpawn": {"x": 100, "y": 500},
  "toySpawn": {"x": 1800, "y": 500},
  "snails": [
    {
      "position": {"x": 500, "y": 400},
      "dialogue": ["Hello!", "Good luck!"]
    }
  ],
  "spiders": [
    {
      "waypoints": [{"x": 5, "y": 3}, {"x": 10, "y": 3}]
    }
  ],
  "food": [
    {"position": {"x": 8, "y": 5}, "type": "BERRY"}
  ],
  "tripwires": [
    {"x": 600, "y": 300}
  ],
  "mechanics": {
    "enemySpeedMultiplier": 1.0,
    "playerStaminaMultiplier": 1.0,
    "darknessFactor": 0.0
  }
}
```

## Tile Constants

| ID | Name | Description |
|----|------|-------------|
| 0 | AIR | Empty space |
| 1 | GROUND | Solid ground |
| 2 | PLATFORM | One-way platform |
| 3-10 | WALLS/CORNERS | Various wall pieces |
| 11-14 | SLOPES | Slope tiles |
| 15 | EXIT | Level exit |

## Troubleshooting

### Editor won't start
- Ensure Java 17+ is installed
- Run `mvn compile` first to check for errors

### Sprites not loading
- Check that `res/sprites/` contains the sprite files
- Placeholders are used if files are missing

### Level won't validate
- Ensure player spawn is set
- Ensure at least one EXIT tile exists
- Check spider waypoints are connected

---

*Last updated: Level Editor v1.0*
