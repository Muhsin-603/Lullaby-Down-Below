# 🕯️ Lullaby Down Below
**A 2D Top-Down Psychological Stealth-Horror Game**

![Java](https://img.shields.io/badge/Java-17-orange) ![Engine](https://img.shields.io/badge/Engine-Custom_Swing-blue) ![License](https://img.shields.io/badge/License-MIT-green) ![PRs](https://img.shields.io/badge/PRs-Welcome-brightgreen)

[Features](#-features) • [Quick Start](#-quick-start) • [Architecture](#-architecture) • [Contributing](#-contributing) • [Roadmap](#-roadmap) • [Team](#-team)

---

## 🎯 What is Lullaby Down Below?
**Lullaby Down Below** is an atmospheric survival horror experience built entirely from scratch using a custom Java Swing engine. There is no Unity, no Godot, no pre-made physics—just pure code.

You play as a lost **baby bug** crawling through the abandoned underground floors of a nursery. Struggling against hunger and darkness, you are guided by a mystical **snail** and hunted by relentless **spiders**.

> *"Do you hear it…? The lullaby calling from beneath the floorboards?"*

## The Experience
Most horror games rely on cheap jump scares. **Lullaby Down Below** relies on tension, resource management, and helplessness.
*   **Hunger System:** If you don't eat, you cry. If you cry, they hear you.
*   **Struggle Mechanic:** Getting caught isn't the end—unless you fail to escape the web.
*   **Stealth:** The shadows are your only weapon against the light.

## Why Play (or Code) This?
✅ **100% Custom Engine** – Built from the ground up to demonstrate core game programming concepts.  
✅ **Advanced AI** – Enemies that patrol, listen, and investigate—they don't just walk at you.  
✅ **Frame-Perfect Timing** – Stable 60 FPS loop with delta-time support.  
✅ **Developer Friendly** – Includes a comprehensive debug suite (F3/F4/F7) built right in.  
✅ **Open Source** – No hidden costs, no subscriptions, just code.

---

## 🧑‍💻 Contribution Workflow
We follow a structured contribution process to keep the codebase clean.
1.  **Browse Issues**: Check for existing bugs or feature requests.
2.  **Open an Issue**: If you have a new idea, discuss it first.
3.  **Wait for Approval**: Maintainers will review your proposal.
4.  **Start Coding**: Once approved, fork and branch.
5.  **Submit PR**: Create a Pull Request with a clear description.

See [CONTRIBUTING.md](CONTRIBUTING.md) for full guidelines.

---

## ✨ Features

### 🔧 Core Tech
<details>
<summary><b>Custom Game Engine</b> (Click to expand)</summary>
<ul>
<li>Stable 60 FPS game loop</li>
<li>Delta-time calculation for smooth movement</li>
<li>Custom collision detection system</li>
<li>State management (Menu -> Play -> Win/Loss)</li>
</ul>
</details>

<details>
<summary><b>Debug System (v1.5.x)</b> (Click to expand)</summary>
<ul>
<li><b>F3:</b> Performance Overlay (FPS, Memory, Entity Count)</li>
<li><b>F4:</b> Hitbox Visualization (Red = Enemy, Green = Player)</li>
<li><b>F6:</b> AI Pathfinding Vectors</li>
<li><b>F7:</b> God Mode & Teleport</li>
<li><b>F12:</b> JSON State Export</li>
</ul>
</details>

### 🕷️ AI & Mechanics
*   **Spider AI:** Uses waypoints, vision cones, and sound detection. They react to the player's "crying" state.
*   **Mystic Snail:** Acts as an emotional support GPS, teleporting to guide the player.
*   **Hazards:** Sticky floors, water currents, and tripwires add environmental challenges.

---

## 🚀 Quick Start
Get the game running locally in minutes.

### 📦 Prerequisites
*   **Java 17** or higher
*   **Maven** (for building from source)

### 🎮 For Players
1.  Download the latest JAR from Releases.
2.  Run: `java -jar lullaby-down-below.jar`
3.  **Survive.**

### ⚙️ For Developers (Build & Run)
```bash
# Clone the repository
git clone https://github.com/yourusername/Lullaby-Down-Below.git

# Navigate to directory
cd Lullaby-Down-Below

# Install dependencies & Build
mvn clean install

# Run the game
mvn exec:java -Dexec.mainClass="com.buglife.main.Game"
```

---

## 🏗️ Architecture

### Tech Stack
*   **Language:** Java 17
*   **Build:** Maven
*   **Logging:** SLF4J + Logback
*   **Data:** Jackson (JSON parsing for levels/config)
*   **Testing:** JUnit 5 + Mockito

### Project Structure
```text
src/com/buglife/
 ├─ assets/       # Image & Sound loaders
 ├─ config/       # JSON configuration handlers
 ├─ engine/       # Core loop, Window, Level Editor
 ├─ entities/     # Game objects (Player, Spider, Snail)
 ├─ main/         # Entry point & State Manager
 ├─ states/       # Game logic states (Playing, Menu)
 └─ world/        # TileMap, Camera, QuadTree
```

---

## 🎮 Controls

| Key | Action |
| :--- | :--- |
| **WASD / Arrows** | Movement |
| **Space** | Struggle (when caught in web) |
| **E** | Interact / Eat |
| **F** | Throw Distraction |
| **Shift** | Dash |
| **Esc** | Pause Menu |

---

## 🗺️ Roadmap

**Phase 1: Foundation (Completed)**
- [x] Core Engine & Loop
- [x] Basic AI Implementation
- [x] Level Loading System
- [x] Maven Integration

**Phase 2: Polish (Current)**
- [x] Debug Overlay (v1.5)
- [ ] Advanced Lighting System
- [ ] Sound Design Overhaul
- [ ] Save/Load System

**Phase 3: Expansion**
- [ ] Level Editor UI
- [ ] Proc-Gen Levels
- [ ] Boss Fights

---

## 📜 Bug Hunter's Log
*Development is messy. Here are some legendary battles we fought:*
*   **The Infinite Cloning Vat:** 60 spiders spawning per second.
*   **The NaN Teleport:** Dividing by zero causes interdimensional travel.
*   **The Zombie State:** Player moving while dead.
*   **The Escaped Quote:** Shell scripts destroying Java strings.

---

## 🤝 Contributing
We welcome contributions! Whether you're fixing logic, optimizing the renderer, or designing levels.

*   Look for issues tagged `good first issue`.
*   Read our [CONTRIBUTING.md](CONTRIBUTING.md) before starting.

---

## ✍️ Team
**Development Team**
*   **Muhsin** – Lead Developer, Engine Architecture
*   **Sai** – AI Programming, Behavior Systems
*   **Rishnu** – Level Design, Maps
*   **Shibili** – Co-Lead, Debugging Tools

**Special Thanks**
*   **Jenny (Gemini)** – Creative Partner & Docs
*   **Andrea (AI)** – InfoHub & Debugging

---

## 🌟 Support
If you enjoyed the code or the scare:
*   ⭐ **Star this repo**
*   🐛 **Report bugs**
*   📢 **Share with friends**

**License:** [MIT](LICENSE) | **Security:** [SECURITY.md](SECURITY.md)

*Do you hear it? The lullaby is calling...*
