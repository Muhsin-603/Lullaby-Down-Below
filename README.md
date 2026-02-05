# 🕯️ Lullaby Down Below
### A 2D Top-Down Psychological Stealth Horror Game

> *"Do you hear it…? The lullaby calling from beneath the floorboards?"*

**Lullaby Down Below** is an atmospheric survival horror game built from scratch using a custom Java Swing engine. You play as a fragile baby bug, crawling through abandoned underground nursery floors, guided by a mysterious snail, hunted by relentless spiders, fighting hunger, darkness… and your own noise.

---

## 📚 Table of Contents
1. [Overview](#-overview)
2. [Features](#-features)
3. [Recent Updates](#-recent-updates-v15x)
4. [For Players](#-for-players)
5. [For Developers](#-for-developers)
6. [Team](#-team)

---

## 🧠 Overview

| Category | Details |
| :--- | :--- |
| **Genre** | Psychological Stealth Horror |
| **Perspective** | 2D Top-Down |
| **Engine** | Custom Java Swing |
| **FPS** | Stable 60 |
| **Levels** | 5 Handcrafted Stages |
| **Focus** | Stealth, survival, tension, AI behavior |

**You are weak by design.**
No weapons. No glory. Just survival instincts and panic.
Basically: **hide, crawl, don’t cry.**

---

## ✨ Features

### 🔧 Custom Engine
* **Stable 60 FPS** delta-time loop.
* **Tile-based maps** parsed from `.txt` files.
* **Viewport Culling** with smooth camera tracking.
* **Virtual Resolution** scaling (1366×768).
* **State Machine** architecture (Menu → Playing → GameOver).
* **Real-time performance monitor**.

### 👶 Core Gameplay
* **Vulnerable Protagonist:** You are small. Everything else is big.
* **Hunger System:** Hunger leads to crying. Crying alerts enemies.
* **Struggle Mechanic:** Got caught? You have a 5-second web death timer.
* **Stealth:** Utilize shadows for invisibility.
* **Hazards:** Sticky floors, water, ladders.

### 🕷️ Spider AI
*Because normal enemies are boring.*
* **Patrol waypoints** & Line-of-sight checks.
* **Sound detection** & Vision cones.
* **Chase + Pathfinding** with investigation behavior.
* **Web immobilization attack**.
* *Note: These spiders don’t "walk toward player.exe". They think. Like rude coworkers.*

### 🐌 Mystic Snail
* Guides player through darkness.
* Teleports between screens.
* **Acts as your emotional support GPS.**

---

## 🚀 Recent Updates (v1.5.x)

**The "I Don't Trust My Code" Debug System:**

| Key | Feature |
| :--- | :--- |
| `F3` | Performance overlay |
| `F2` | Spider debug menu |
| `F4` | Hitboxes |
| `F5` | Grid overlay / Restart |
| `F6` | Patrol paths / Cycle levels |
| `F7` | God mode / Teleport |
| `F12` | Export game state |

---

## 🎮 For Players

### Installation
**Easy Mode:** Download JAR → double-click → survive.

**From Source:**
```bash
java -jar target/lullaby-down-below-1.5.0-jar-with-dependencies.jar

Controls
| Key | Action |
|---|---|
| WASD / Arrows | Move |
| Space | Struggle (when caught) |
| E | Interact |
| F | Throw Item |
| Shift | Dash |
| Esc | Pause |
🧩 Gameplay Tips
 * Hunger = Life. Eat or you cry. Cry and you die.
 * Stay in Shadows. Light is your enemy.
 * Escape Webs Fast. Mash Space like you mean it.
 * Follow the Snail. It knows the way.
🛠️ For Developers
Stack
 * Java 17
 * Maven
 * SLF4J + Logback
 * Jackson (JSON parsing)
 * JUnit + Mockito
Build & Run
git clone [https://github.com/yourusername/Lullaby-Down-Below.git](https://github.com/yourusername/Lullaby-Down-Below.git)
cd Lullaby-Down-Below
mvn clean install

# Run Main Class
mvn exec:java -Dexec.mainClass="com.buglife.main.Game"

Configuration
Located in config.json:
{
  "game": {
    "targetFPS": 60,
    "enableDebug": false
  }
}

Project Structure
src/com/buglife/
 ├─ assets/       # Image loaders
 ├─ config/       # JSON handlers
 ├─ entities/     # Player, Spider, Snail
 ├─ main/         # Game loop & Window
 ├─ states/       # Menu, Play, GameOver
 ├─ ui/           # HUD, Overlay
 └─ world/        # TileMap, Camera

🧪 Debugging Guide
If the game crashes:
 * Press F3 → Check FPS.
 * Check logs (SLF4J).
 * Verify JDK version.
 * If it still fails: Stare at the wall dramatically. Works 30% of the time.
📜 Bug Hunter’s Log
Legendary battles fought during development:
 * The Package Curse
 * Infinite Spider Cloning
 * The NaN Teleport Glitch
 * Zombie State Machine
 * The "Escaped Quotes" Disaster
✍️ Team
 * Muhsin – Lead Dev
 * Sai – AI Logic
 * Rishnu – Level Design
 * Shibili – Debugger
 * Jenny – Chaos Consultant & Sarcastic Life Support
 * Andrea – Knowledge Oracle
License: See LICENSE.
Security: See SECURITY.md.

