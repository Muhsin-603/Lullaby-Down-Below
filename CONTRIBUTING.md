# Contributing to Lullaby Down Below

Thank you for your interest in contributing to **Lullaby Down Below**! We welcome contributions from the community to help make this atmospheric horror game even better.

Whether you're fixing bugs, improving documentation, or proposing new features, your help is appreciated.

## 📋 Table of Contents
- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [How to Contribute](#how-to-contribute)
  - [Reporting Bugs](#reporting-bugs)
  - [Suggesting Enhancements](#suggesting-enhancements)
  - [Pull Requests](#pull-requests)
- [Development Workflow](#development-workflow)
- [Style Guide](#style-guide)

## 🤝 Code of Conduct
Please be respectful and considerate in all interactions. We want to maintain a welcoming environment for everyone.

## 🚀 Getting Started

1.  **Fork the repository** on GitHub.
2.  **Clone the project** to your local machine:
    ```bash
    git clone https://github.com/YourUsername/Lullaby-Down-Below.git
    cd Lullaby-Down-Below
    ```
3.  **Install dependencies** (Maven is required):
    ```bash
    mvn clean install
    ```
4.  **Run the game** to verify everything is working:
    - You can use the provided batch scripts (`build-dev.bat`, `run-level-editor.bat`).
    - Or run directly via Java/Maven.

## 🛠️ How to Contribute

### Reporting Bugs
If you find a bug, please create a new issue on GitHub.
*   **Check existing issues** to see if it has already been reported.
*   **Use the Bug Report template** if available.
*   **include details**:
    *   Steps to reproduce the bug.
    *   What you expected to happen vs. what actually happened.
    *   Screenshots or logs if applicable (check `logs/` or `debug-settings.json`).
    *   Your OS and Java version.

### Suggesting Enhancements
Have an idea for a new feature or level mechanics?
*   Open an issue with the tag `enhancement`.
*   Clearly describe the feature and why it would be beneficial.
*   Mockups or diagrams are helpful!

### Pull Requests
1.  **Create a branch** for your feature or fix:
    ```bash
    git checkout -b feature/amazing-new-feature
    # or
    git checkout -b fix/annoying-bug
    ```
2.  **Make your changes**. Keep them focused and minimal.
3.  **Test your changes**. Ensure the game builds and runs correctly.
4.  **Commit your changes** with descriptive messages:
    ```bash
    git commit -m "Fix collision issue with Snail entity"
    ```
5.  **Push to your fork**:
    ```bash
    git push origin feature/amazing-new-feature
    ```
6.  **Open a Pull Request (PR)** against the `main` branch of the original repository.
    *   Reference any related issues in your PR description (e.g., "Closes #123").
    *   Wait for review and address any feedback.

## 💻 Development Workflow
*   **Build System**: We use Maven. `pom.xml` handles dependencies.
*   **Project Structure**: Source code is in `src/com/buglife/...`. Resources (assets) are in `res/`.
*   **Level Editor**: Use `run-level-editor.bat` to test map changes or creates new levels.

## 🎨 Style Guide
*   **Java**: Follow standard Java naming conventions (CamelCase for classes, camelCase for variables/methods).
*   **Indentation**: Use tabs or 4 spaces (consistent with existing files).
*   **Comments**: Add Javadoc for complex methods and classes.
*   **Logging**: Use SLF4J/Logback. Avoid `System.out.println`.

---
*Happy Coding, and good luck in the dark!* 🕯️
