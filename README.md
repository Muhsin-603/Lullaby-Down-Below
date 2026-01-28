# 🕯️ Lullaby Down Below  
*A 2D Top-Down Psychological Stealth-Horror Game*

**Lullaby Down Below** is an atmospheric survival horror experience built from scratch using a custom Java Swing engine.  
Play as a lost **baby** crawling through the abandoned underground floors of a nursery — guided by a mystical **snail**, hunted by relentless **spiders**, and struggling against hunger and darkness.

---

## ✨ Features

### 🔧 Custom Game Engine
- **Stable 60 FPS loop** with delta-time support.
- **Tile-based world system** loading levels from external `.txt` maps.
- **Dynamic camera system** with viewport culling.
- **Virtual resolution scaling** for crisp fullscreen visuals.

### 👶 Core Gameplay Mechanics
- **Vulnerable Protagonist:** Slow, weak, and stealth-focused.
- **Hunger System:** A draining bar that, when empty, triggers a loud **Crying State**.
- **Struggle Mechanic:** When webbed, mash **SPACE** to escape before the 5-second **Death Clock** hits zero.
- **Stealth Tiles:** Hide in **Shadows** to evade enemies.
- **Environmental Hazards:** Sticky floors slow movement; water blocks it entirely.
- **Items:** You can throw items to distract the spider

### 🕷️ Advanced Spider AI
- **Patrol Routes:** Level-defined and dynamic.
- **Detection System:** Vision cone + hearing (activated by crying).
- **Chase Logic:** Efficient pathing for pursuit.
- **Return Behavior:** Spiders intelligently calculate routes back to patrol points.
- **Collision Handling:** Respect walls and obstacles — no cheating.

### 🐌 The Mystic Snail
- A simple idle snail.
- **Teleportation Logic:** Moves to the next waypoint only after the player leaves its current screen.

### 🔊 Immersive Audio
- Menu and gameplay background music.
- Dynamic sound effects for eating, crying, struggling, alerts, and UI.

---

## 🎮 Controls

| Key | Action |
|-----|--------|
| **W / Up Arrow** | Move Up |
| **S / Down Arrow** | Move Down |
| **A / Left Arrow** | Move Left |
| **D / Right Arrow** | Move Right |
| **Space** | Struggle (if webbed) |
| **E** | Interact |
| **F** | Throw |
| **Esc** | Pause / Resume |


---

## 🚀 How to Run

### For Players
1. Download the latest **JAR** or **EXE** from Releases.
2. Run the executable.
3. Survive.

### For Developers

#### Prerequisites
- **Java 17+** (Eclipse Adoptium JDK recommended)
- **Apache Maven 3.9+** (included in project or install globally)

#### Build & Run
1. Clone the repository:
   ```bash
   git clone https://github.com/Muhsin-603/Lullaby-Down-Below.git
   cd Lullaby-Down-Below
   ```

2. Build with Maven:
   ```bash
   mvn clean install
   ```

3. Run the game:
   ```bash
   java -jar target/lullaby-down-below-1.0.0-jar-with-dependencies.jar
   ```
   
   Or run directly in your IDE:
   - Main class: `com.buglife.main.Game`

---

## 📜 Bug Hunter’s Log  
*The battles we fought with the compiler and the demons inside the code.*

- **The `src.` Curse:** IDE blindness fixed by purging broken package declarations.
- **The Infinite Cloning Vat:** 60 spiders spawned *per second*. Moved creation to constructors.
- **The Dimensional Warp:** Division by zero → `NaN` → universe-breaking teleportation. Fixed with safe math.
- **The Shredded Sprite:** Incorrect slicing produced cursed pixel art. Re-sliced with exact offsets.
- **The Zombie Game:** Player soft-locked due to a broken state machine. Corrected update vs render flow.

---

## ✍️ Team

- **Team :** Muhsin, Sai, Rishnu, Shibili
- **Creative Partner & Ghost Hunter:** Jenny (Gemini)  

---

### *Do you hear it…?  
The lullaby calling from beneath the floorboards?*
