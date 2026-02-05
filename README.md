🕯️ Lullaby Down Below

A 2D Top-Down Psychological Stealth Horror Game

Lullaby Down Below is an atmospheric survival horror game built from scratch using a custom Java Swing engine.

You play as a fragile baby bug, crawling through abandoned underground nursery floors, guided by a mysterious snail, hunted by relentless spiders, fighting hunger, darkness… and your own noise.

> “Do you hear it…? The lullaby calling from beneath the floorboards?”




---

📚 Table of Contents

1. Overview


2. Features


3. Recent Updates (v1.5.x)


4. For Players


5. Gameplay Guide


6. For Developers


7. Configuration


8. Project Structure


9. Debugging Guide


10. Bug Hunter’s Log


11. Team


12. License & Security




---

🧠 Overview

Category	Details

Genre	Psychological Stealth Horror
Perspective	2D Top-Down
Engine	Custom Java Swing
FPS	Stable 60
Levels	5 handcrafted stages
Focus	Stealth, survival, tension, AI behavior


You are weak by design.
No weapons. No glory. Just survival instincts and panic.

Basically: hide, crawl, don’t cry.


---

✨ Features

🔧 Custom Engine

Stable 60 FPS delta-time loop

Tile-based maps from .txt files

Smooth camera with viewport culling

Virtual resolution scaling (1366×768)

State machine (Menu → Playing → GameOver)

Real-time performance monitor



---

👶 Core Gameplay

Vulnerable protagonist

Hunger system → crying alerts enemies

Struggle mechanic (5s web death timer)

Stealth shadows for invisibility

Hazards: sticky floors, water, ladders

Items: food, toys, tripwires



---

🕷️ Spider AI

Because normal enemies are boring.

Patrol waypoints

Vision cones

Sound detection

Line-of-sight checks

Chase + pathfinding

Investigation behavior

Web immobilization attack


These spiders don’t “walk toward player.exe”.
They think. Like rude coworkers.


---

🐌 Mystic Snail

Guides player through darkness

Teleports between screens

Acts as living waypoint


Yes. A snail is your emotional support GPS.


---

🔊 Audio

Dynamic music

Player SFX (crying, eating, struggling)

Spider alerts

Environmental cues


Play with headphones unless you enjoy surprise heart attacks.


---

🚀 Recent Updates (v1.5.x)

Advanced Debug System

Key	Feature

F3	Performance overlay
F2	Spider debug menu
F4	Hitboxes
F5	Grid overlay / Restart
F6	Patrol paths / Cycle levels
F7	God mode / Teleport
F8	Spawn food
F12	Export game state


Extras:

Config persistence

Entity counter

Cleaner HUD

Real-time AI toggles


You basically built a dev console that screams “I don’t trust my own code and that’s healthy.”


---

🎮 For Players

Requirements

Java 17+

512MB RAM

1366×768 resolution



---

Installation

Easy

Download JAR → double-click → survive.

From Source

java -jar target/lullaby-down-below-1.5.0-jar-with-dependencies.jar


---

Controls

Key	Action

WASD / Arrows	Move
Space	Struggle
E	Interact
F	Throw
Shift	Dash
Esc	Pause
F3	Debug overlay



---

🧩 Gameplay Tips

1. Hunger = life


2. Stay in shadows


3. Toys are precious


4. Listen carefully


5. Escape webs fast


6. Follow the snail



Ignore these and the spiders will hold a memorial service for you.


---

🛠️ For Developers

Stack

Java 17

Maven

SLF4J + Logback

Jackson

JUnit + Mockito



---

Build

git clone <repo>
cd Lullaby-Down-Below
mvn clean install

Run:

mvn exec:java -Dexec.mainClass="com.buglife.main.Game"


---

⚙️ Configuration

config.json

{
  "game": {
    "targetFPS": 60,
    "enableDebug": false
  }
}


---

Tile IDs

0 Floor
1 Wall
2 Shadow
3 Water
4 Sticky
5-7 Ladder
8 Food
9 Player
10 Spider
11 Snail

Simple. Brutal. Effective.


---

📂 Project Structure

src/com/buglife/
 ├─ assets/
 ├─ config/
 ├─ entities/
 ├─ main/
 ├─ states/
 ├─ ui/
 ├─ utils/
 └─ world/

res/
 ├─ maps/
 ├─ sounds/
 ├─ sprites/

Everything cleanly modular.
Past-you actually used architecture instead of vibes. Growth.


---

🧪 Debugging Guide

Press F3 → check FPS

Check logs (SLF4J)

Verify JDK version

Reload language server if imports break


If it still fails, stare at the wall dramatically. Works 30% of the time.


---

📜 Bug Hunter’s Log

Legendary battles:

Package curse

Infinite spider cloning

NaN teleport glitch

Shredded sprites

Zombie state machine

Fake IOException

Escaped quotes disaster


Classic “Java horror anthology.”


---

✍️ Team

Development

Muhsin – Lead Dev

Sai – AI

Rishnu – Level Design

Shibili – Debugger


Special

Jenny – chaos consultant and sarcastic life support

Andrea – knowledge oracle



---

📄 License

See LICENSE.

🔒 Security

See SECURITY.md.

