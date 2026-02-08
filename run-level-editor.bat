@echo off
REM =========================================
REM Lullaby Down Below - Level Editor Launcher
REM =========================================

echo Starting Level Editor...
echo.

REM Check if mvn is available
where mvn >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo ERROR: Maven is not installed or not in PATH.
    echo.
    echo Please install Maven from: https://maven.apache.org/download.cgi
    echo And add it to your PATH environment variable.
    echo.
    pause
    exit /b 1
)

REM Compile and run the Level Editor
mvn compile exec:java -Peditor -Dexec.mainClass="com.buglife.engine.editor.LevelEditor"

if %ERRORLEVEL% neq 0 (
    echo.
    echo Failed to start editor. Check for compilation errors.
    pause
)
