# Map Preview Tool

A standalone level design utility for **Lullaby Down Below** that renders maps at low resolution with live updates, entity markers, and helpful design tools.

## 🚀 Quick Start

### Option 1: Run from IDE
Right-click on `MapPreviewTool.java` and select **Run** (works in IntelliJ, Eclipse, VS Code with Java extensions).

### Option 2: Run from Command Line
```bash
# First, compile the project
mvn compile

# Then run the tool
java -cp "target/classes;res" com.buglife.tools.MapPreviewTool
```

### Option 3: Use the Batch Script (Windows)
```
run-map-preview.bat
```

---

## ✨ Features

### 🗺️ Map Rendering
- **Low-resolution preview** - Symbolic colored tiles (no sprite loading needed)
- **Solid tile highlighting** - Walls and obstacles have white borders
- **Coordinate overlay** - Shows tile numbers along edges
- **Grid lines** - Toggle-able grid for precision placement

### 🎯 Entity Markers
| Marker | Color | Description |
|--------|-------|-------------|
| 🐌 Snail | Cyan | Shows dialogue count as number |
| 🕷 Spider | Red | Shows patrol path with numbered waypoints |
| 🍎 Berry | Yellow | Food spawn (restores hunger) |
| ⚡ Energy Seed | Green | Speed boost food |
| ⚡ Tripwire | Orange | Horizontal line marker |
| 🎮 Player | Blue "P" | Player spawn point |
| 🧸 Toy | Magenta "T" | Toy spawn point |
| 🚪 Exit | Bright green | Level complete tile (Ladder 3) |

### 🔄 Live Reload
- **Auto-watches map files** for changes
- Edit `res/maps/levelX.txt` and see updates instantly
- Toggle on/off in Options tab

### 📏 Measurement Tool
- Click **📏 Measure** button or press **M**
- Click two points to measure distance
- Shows tile distance + pixel distance + diagonal
- Press **Escape** to exit measure mode

### 📊 Statistics Tab
- Total tile count
- Solid vs walkable percentage
- Pixel dimensions
- Tile usage breakdown

### 🎨 Tile Palette Tab
- Visual reference for all tile IDs
- Red border = solid tile
- Hover for tooltip with full info

### 📋 Entity Inspector Tab
- Complete list of all entities in level
- Player spawn, toy spawn, snails, spiders, food, tripwires
- Mechanics enabled (dash, toy, speed boost food)

---

## ⌨️ Keyboard Shortcuts

| Key | Action |
|-----|--------|
| **1-5** | Quick switch to level 1-5 |
| **T** | Switch to test level |
| **R** | Reload current level |
| **F** | Fit map to window |
| **M** | Toggle measure mode |
| **Escape** | Exit measure mode / clear selection |
| **Arrow keys** | Pan the view |
| **Mouse wheel** | Zoom in/out |
| **Right-drag** | Pan the view |

---

## 🎯 Level Design Workflow

1. **Open the tool** alongside your code editor
2. **Select your level** from the dropdown (or press 1-5)
3. **Edit the map file** (`res/maps/levelX.txt`) - changes appear instantly
4. **Edit LevelXConfig.java** to adjust spawns
5. **Press R** to reload after config changes
6. **Use Entities tab** to verify your spawn data
7. **Use Measure tool** to check distances between points
8. **Export PNG** for documentation or sharing

---

## 📁 File Locations

| File Type | Location |
|-----------|----------|
| Map data | `res/maps/level1.txt` ... `level5.txt` |
| Level configs | `src/com/buglife/levels/Level1Config.java` ... |
| This tool | `src/com/buglife/tools/MapPreviewTool.java` |

---

## 🎨 Tile Color Legend

The preview uses symbolic colors instead of sprites for fast loading:

- **Floor (0)**: Dark brown
- **Wall (1, 2)**: Light brown (solid)
- **Sticky (3)**: Yellow-brown
- **Shadow (5)**: Very dark
- **Stains (6-9)**: Slightly lighter floor
- **Planks (31-34)**: Light tan
- **Ladders (35-38)**: Brown (37 = exit, bright green)
- **Sacks (11-14)**: Tan-brown (mostly solid)
- **Intro tiles (41-46)**: Gray-blue

---

## 💡 Tips

- Use **Stats tab** to check if your level has reasonable solid/walkable ratio
- Spider patrol waypoints are in **tile coordinates** (not pixels)
- Snail positions are in **pixel coordinates**
- The **description** field in spawn data shows as a label on hover
- Export PNG for your design docs or to share level layouts
