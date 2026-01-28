# 🕯️ Lullaby Down Below  
*A 2D Top-Down Psychological Stealth-Horror Game*

**Lullaby Down Below** is an atmospheric survival horror experience built from scratch using a custom Java Swing engine.  
Play as a lost **baby bug** crawling through the abandoned underground floors of a nursery — guided by a mystical **snail**, hunted by relentless **spiders**, and struggling against hunger and darkness.

> *"Do you hear it…? The lullaby calling from beneath the floorboards?"*

---

## 📖 Table of Contents
- [Features](#-features)
- [For Players](#-for-players)
- [For Developers](#-for-developers)
- [Project Structure](#-project-structure)
- [Bug Hunter's Log](#-bug-hunters-log)
- [Team](#-team)

---

## ✨ Features

### 🔧 Custom Game Engine
- **Stable 60 FPS game loop** with delta-time support and frame-perfect timing
- **Tile-based world system** loading levels from external `.txt` map files
- **Dynamic camera system** with smooth viewport culling and boundary detection
- **Virtual resolution scaling** (1366x768) with crisp fullscreen rendering
- **Performance monitoring** with real-time FPS counter (toggle with F3)
- **State management system** for seamless transitions between menu, playing, and game over states

### 👶 Core Gameplay Mechanics
- **Vulnerable Protagonist:** You're a baby bug — slow, weak, and stealth-focused
- **Hunger System:** A constantly draining bar that triggers **Crying State** when empty, alerting all enemies
- **Struggle Mechanic:** When caught in a spider's web, mash **SPACE** to escape before the 5-second death timer expires
- **Stealth Tiles:** Hide in **Shadows** to become invisible to patrolling spiders
- **Environmental Hazards:**
  - **Sticky Floors:** Slow your movement speed
  - **Water Tiles:** Block movement entirely
  - **Ladder Tiles:** Navigate between floors
- **Interactive Items:**
  - **Food:** Restore hunger and survive longer
  - **Toys:** Throw to distract spiders and create escape opportunities
  - **Trip Wires:** Trigger spider investigations when crossed

### 🕷️ Advanced Spider AI
- **Patrol Routes:** Waypoint-based patrol system defined in level files
- **Multi-Layered Detection:**
  - Vision cone detection with configurable range and angle
  - Sound-based hearing triggered by baby's crying state
  - Line-of-sight checks respecting walls and obstacles
- **Chase Behavior:** Intelligent pathfinding pursuit when player is detected
- **Investigation Mode:** Spiders investigate last known positions and disturbances
- **Return Behavior:** Smart route calculation back to patrol points after losing the player
- **Web Attack:** Immobilize the player for a deadly 5-second struggle sequence

### 🐌 The Mystic Snail
- **Guide Through Darkness:** Acts as a waypoint marker leading you through the nursery
- **Screen-Based Teleportation:** Moves to the next waypoint only after the player leaves its screen
- **Visual Indicator:** Helps players navigate the complex multi-level world

### 🔊 Immersive Audio System
- **Dynamic Music:** Separate tracks for menu and gameplay states
- **Rich Sound Effects:**
  - Player eating, crying, struggling, and dying
  - Spider alert, patrol, and attack sounds
  - UI interaction feedback
  - Environmental audio cues

---

## 🎮 For Players

### System Requirements
- **OS:** Windows 10/11, macOS 10.14+, Linux
- **Java:** Java Runtime Environment (JRE) 17 or higher
- **Memory:** 512 MB RAM minimum
- **Display:** 1366x768 or higher resolution recommended

### Installation & Running

#### Option 1: Executable Release (Easiest)
1. Go to [Releases](https://github.com/Muhsin-603/Lullaby-Down-Below/releases)
2. Download the latest **JAR** file or platform-specific **EXE**
3. Double-click the file to run
4. **Survive.**

#### Option 2: From Source
If you don't have Java installed:
1. Download and install [Java 17+](https://adoptium.net/)
2. Download the source code
3. Run:
   ```bash
   java -jar target/lullaby-down-below-1.0.0-jar-with-dependencies.jar
   ```

### Controls

| Key | Action |
|-----|--------|
| **W / ↑** | Move Up |
| **S / ↓** | Move Down |
| **A / ←** | Move Left |
| **D / →** | Move Right |
| **Space** | Struggle (when webbed) |
| **E** | Interact with objects (pick up food/toys) |
| **F** | Throw item to distract spiders |
| **Shift** | Dash (costs hunger) |
| **Esc** | Pause / Resume game |
| **F3** | Toggle performance overlay (FPS counter) |

### Gameplay Tips
1. **Manage Hunger:** Always keep an eye on your hunger bar. When it hits zero, you'll cry and attract ALL spiders.
2. **Use Shadows Wisely:** Stay in shadow tiles to become invisible. This is your best defense.
3. **Save Your Toys:** Don't waste toys on spiders that aren't actively chasing you. Use them strategically.
4. **Listen Carefully:** Audio cues warn you when spiders detect you or enter alert mode.
5. **Escape is Everything:** When webbed, mash Space frantically. You have 5 seconds before death.
6. **Follow the Snail:** The mystical snail guides you through each level. Trust its path.

### Level Progression
- **5 Levels** of increasing difficulty
- Each level introduces new layout challenges and spider patrol patterns
- Complete levels by reaching the snail at designated waypoints
- Death returns you to the current level's starting position

---

## 🛠️ For Developers

### Prerequisites
- **Java Development Kit (JDK) 17+** ([Eclipse Adoptium](https://adoptium.net/) recommended)
- **Apache Maven 3.9+** (included in project, or [install globally](https://maven.apache.org/download.cgi))
- **Git** for version control
- **IDE:** VS Code (with Java Extension Pack), IntelliJ IDEA, or Eclipse

### Quick Start

#### 1. Clone the Repository
```bash
git clone https://github.com/Muhsin-603/Lullaby-Down-Below.git
cd Lullaby-Down-Below
```

#### 2. Build with Maven
```bash
mvn clean install
```
This will:
- Download all dependencies (SLF4J, Logback, Jackson)
- Compile 24 source files
- Run unit tests
- Package into JAR files in `target/` directory

#### 3. Run the Game

**Option A: Using Maven**
```bash
mvn exec:java -Dexec.mainClass="com.buglife.main.Game"
```

**Option B: Direct JAR Execution**
```bash
java -jar target/lullaby-down-below-1.5.0-jar-with-dependencies.jar
```

**Option C: From IDE**
- Open the project in your IDE
- Navigate to `src/com/buglife/main/Game.java`
- Run the `main()` method
- Main class: `com.buglife.main.Game`

### Dependencies
Managed automatically by Maven (`pom.xml`):

| Dependency | Version | Purpose |
|------------|---------|---------|
| **SLF4J API** | 2.0.9 | Logging facade |
| **Logback Classic** | 1.4.14 | Logging implementation |
| **Jackson Databind** | 2.16.0 | JSON/YAML parsing for configs |
| **JUnit Jupiter** | 5.10.1 | Unit testing framework |
| **Mockito** | 5.8.0 | Mock testing library |

### Configuration Files

#### `config.json` (Root Directory)
```json
{
  "game": {
    "targetFPS": 60,
    "enableDebug": false
  }
}
```
- Modify `targetFPS` to change game speed (30-120 recommended range)
- Enable `enableDebug` for verbose logging

#### Map Files (`res/maps/level1.txt` - `level5.txt`)
Text-based tile maps using numerical IDs:
```
0 = Floor
1 = Wall
2 = Shadow (stealth tile)
3 = Water (blocking)
4 = Sticky Floor (slow movement)
5-7 = Ladder variants
8 = Food spawn point
9 = Player spawn point
10 = Spider spawn point
11 = Snail waypoint
```

### Development Commands

```bash
# Clean build artifacts
mvn clean

# Compile only (no tests)
mvn compile

# Run tests
mvn test

# Package without running tests
mvn package -DskipTests

# Generate JAR with dependencies
mvn assembly:single

# Run with debug logging
mvn exec:java -Dexec.mainClass="com.buglife.main.Game" -Dlogback.configurationFile=logback-debug.xml
```

### Debugging Tips

**Enable Performance Monitor:**
- Press **F3** in-game to see FPS and update times
- Located in `src/com/buglife/utils/PerformanceMonitor.java`

**Check Logs:**
- Console output shows SLF4J/Logback messages
- Errors are logged with stack traces
- Modify verbosity in `src/main/resources/logback.xml`

**Common Issues:**
1. **Import errors after clone:** Reload Java Language Server (Ctrl+Shift+P → "Java: Clean Java Language Server Workspace")
2. **Maven not found:** Ensure Maven bin directory is in your PATH
3. **JDK version mismatch:** Verify Java 17+ with `java -version`

---

## 📂 Project Structure

```
Lullaby-Down-Below/
│
├── src/com/buglife/          # Source code (Java 17)
│   ├── assets/               # Asset management
│   │   ├── AssetManager.java       # Image loading & caching
│   │   └── SoundManager.java       # Audio playback system
│   │
│   ├── config/               # Configuration & constants
│   │   ├── ConfigManager.java      # JSON/YAML config loader
│   │   ├── GameConstants.java      # Gameplay balance constants
│   │   └── TileConstants.java      # Tile type definitions
│   │
│   ├── entities/             # Game objects
│   │   ├── Player.java             # Main character (hunger, dash, webbed state)
│   │   ├── Spider.java             # Enemy AI (patrol, chase, detect, web)
│   │   ├── Snail.java              # Guide entity with teleport logic
│   │   ├── Food.java               # Hunger restoration item
│   │   ├── Toy.java                # Distraction throwable item
│   │   └── TripWire.java           # Proximity detection trigger
│   │
│   ├── main/                 # Core engine
│   │   ├── Game.java               # Main class, game loop, JFrame setup
│   │   ├── GamePanel.java          # JPanel rendering surface
│   │   └── GameStateManager.java   # State machine controller
│   │
│   ├── states/               # Game states
│   │   ├── GameState.java          # Abstract state interface
│   │   ├── MenuState.java          # Main menu
│   │   ├── PlayingState.java       # Active gameplay state
│   │   ├── LevelCompleteState.java # Level victory screen
│   │   └── GameOverState.java      # Death screen
│   │
│   ├── ui/                   # User interface
│   │   └── MainMenu.java           # Menu rendering & interaction
│   │
│   ├── utils/                # Utility classes
│   │   ├── PerformanceMonitor.java # FPS tracking & debug overlay
│   │   └── QuadTree.java           # Spatial partitioning for collision
│   │
│   └── world/                # Level management
│       ├── World.java              # Tile map loader & collision detection
│       └── Tile.java               # Individual tile properties
│
├── res/                      # Game resources (bundled in JAR)
│   ├── fonts/
│   │   └── Tiny5.ttf               # Custom pixel font
│   ├── maps/
│   │   ├── level1.txt              # Level 1 tile layout
│   │   ├── level2.txt              # Level 2 tile layout
│   │   ├── level3.txt              # Level 3 tile layout
│   │   ├── level4.txt              # Level 4 tile layout
│   │   └── level5.txt              # Level 5 tile layout
│   ├── sounds/
│   │   └── game_theme.wav          # Background music
│   └── sprites/
│       ├── player/                 # Baby bug sprite sheets
│       ├── spider/                 # Spider enemy animations
│       ├── snail/                  # Snail guide sprites
│       ├── tiles/                  # Environment tilesets
│       ├── items/                  # Food, toy, tripwire sprites
│       └── ui/                     # Menu and HUD elements
│
├── target/                   # Build output (generated by Maven)
│   ├── classes/                    # Compiled .class files
│   ├── lullaby-down-below-1.0.0.jar                      # Standard JAR
│   └── lullaby-down-below-1.0.0-jar-with-dependencies.jar # Runnable JAR
│
├── docs/
│   └── notes.txt             # Development notes
│
├── pom.xml                   # Maven configuration (dependencies, build)
├── config.json               # Runtime game configuration
├── .gitignore                # Git exclusion rules
├── LICENSE                   # Project license
├── README.md                 # This file
└── SECURITY.md               # Security policy
```

### Key Components Explained

**Game Loop (`Game.java`):**
- Fixed timestep at 60 FPS (configurable via `config.json`)
- Decoupled update and render cycles
- Fullscreen window management with GraphicsDevice

**State Machine (`GameStateManager.java`):**
- Manages transitions: Menu → Playing → LevelComplete/GameOver → Menu
- Each state handles its own input, update, and rendering logic

**World System (`World.java`):**
- Loads tile maps from text files (e.g., `level1.txt`)
- Provides collision detection via tile properties
- Supports 30+ tile types defined in `TileConstants.java`

**Spider AI (`Spider.java`):**
- Finite state machine: PATROL → ALERT → CHASE → INVESTIGATE → RETURN
- Line-of-sight raycasting for vision cone
- A* pathfinding for navigation (simplified grid-based)

**Asset Management (`AssetManager.java`):**
- Lazy-loading image cache with `/res/` classpath support
- Prevents duplicate loading of sprites
- Returns `null` on missing assets (logged via SLF4J)

**Configuration System (`ConfigManager.java`):**
- Singleton pattern for global settings access
- Reads `config.json` using Jackson databind
- Provides type-safe getters with fallback defaults

---

## 📜 Bug Hunter's Log  
*The battles we fought with the compiler and the demons inside the code.*

### Major Bugs Fixed During Development

#### 1. **The `src.` Package Curse**
**Symptom:** All imports broken in IDE despite successful Maven builds.  
**Cause:** Used `package src.com.buglife.*` instead of standard `package com.buglife.*`  
**Solution:** Mass refactor via PowerShell to remove `src.` prefix from all 24 Java files.  
**Lesson:** Package names should match directory structure, not include the source folder name.

#### 2. **The Infinite Cloning Vat**
**Symptom:** 60 spiders spawned *per second*, causing catastrophic lag and memory exhaustion.  
**Cause:** Spider instantiation logic placed in game loop update instead of level initialization.  
**Solution:** Moved spider creation to level constructors with proper singleton patterns.  
**Lesson:** Understand the difference between initialization and per-frame update logic.

#### 3. **The Dimensional Warp**
**Symptom:** Players randomly teleporting to `(NaN, NaN)`, breaking through walls.  
**Cause:** Division by zero in vector normalization: `dx / 0.0 = NaN`.  
**Solution:** Added safe math checks before division: `if (magnitude > 0)`.  
**Lesson:** Always validate denominators and handle edge cases in physics calculations.

#### 4. **The Shredded Sprite Apocalypse**
**Symptom:** Player sprite appeared as a corrupted, garbled mess of pixels.  
**Cause:** Incorrect sprite sheet slicing offsets (off by 1 pixel in X/Y calculations).  
**Solution:** Re-measured sprite dimensions and fixed `BufferedImage.getSubimage()` parameters.  
**Lesson:** Use constants for sprite dimensions and verify slice coordinates visually.

#### 5. **The Zombie Game State**
**Symptom:** Player could move in game over screen; controls unresponsive in playing state.  
**Cause:** State machine calling `update()` on inactive states; wrong state receiving input.  
**Solution:** Implemented proper state activation/deactivation and single-state-at-a-time architecture.  
**Lesson:** State machines require strict gating of input and update calls.

#### 6. **The IOException That Never Was**
**Symptom:** Maven build failure: "IOException never thrown in body of corresponding try statement."  
**Cause:** AssetManager returns `null` on failure but doesn't throw checked exceptions.  
**Solution:** Changed `catch (IOException e)` to `catch (Exception e)` in asset loading code.  
**Lesson:** Read method signatures carefully; not all failure modes use exceptions.

#### 7. **The Escaped Quote Catastrophe**
**Symptom:** Compilation errors in logging statements: "unclosed string literal."  
**Cause:** PowerShell batch operations introduced `\"` escaped quotes in Java strings.  
**Solution:** Fixed all `logger.error(\"Failed...` to `logger.error("Failed...`.  
**Lesson:** Be cautious with shell script find/replace operations on code files.

---

## ✍️ Team

**Development Team:**
- **Muhsin** – Lead Developer, Game Engine Architecture
- **Sai** – AI Programming, Spider Behavior Systems
- **Rishnu** – Level Design, Map Creation
- **Shibili** – Asset Integration, Sound Design

**Special Thanks:**
- **Jenny (Gemini)** – Creative Partner, Documentation, Debugging Assistance

---

## 📄 License

This project is licensed under the terms specified in [LICENSE](LICENSE).

---

## 🔒 Security

Please review our [SECURITY.md](SECURITY.md) for information on reporting vulnerabilities.

---

### *Do you hear it…?  
The lullaby calling from beneath the floorboards?*

**Survive. Escape. Crawl into the light.**
