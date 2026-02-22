@echo off
REM =====================================================
REM  Lullaby Down Below - Release Build Script
REM  Creates a distributable .jar for players
REM =====================================================
REM
REM  This build EXCLUDES all developer tools:
REM    Level Editor, Map Preview Tool, Debug Exporter,
REM    Performance Monitor, Telemetry Client, Test Level
REM
REM  Only player-facing content is included.
REM =====================================================

REM ============== CONFIGURATION ==========================
REM
REM  Change this number to set how many levels are
REM  included in the release build. (1 to 5)
REM
SET MAX_LEVELS=2
REM
REM =======================================================

setlocal enabledelayedexpansion

SET JAR_NAME=lullaby-down-below-1.5.0-release.jar
SET JAR_FILE=target\%JAR_NAME%

echo.
echo =====================================================
echo   Lullaby Down Below - Release Build
echo =====================================================
echo.
echo   Levels included : 1 to %MAX_LEVELS%
echo.
echo   Excluded from build:
echo     - Test Level
echo     - Level Editor
echo     - Map Preview Tool
echo     - Debug Exporter
echo     - Performance Monitor
echo     - Telemetry Client
echo.

REM --- Validate MAX_LEVELS ---
if %MAX_LEVELS% LSS 1 (
    echo ERROR: MAX_LEVELS must be between 1 and 5.
    pause
    exit /b 1
)
if %MAX_LEVELS% GTR 5 (
    echo ERROR: MAX_LEVELS must be between 1 and 5.
    pause
    exit /b 1
)

REM --- Step 1: Build JAR with Maven release profile ---
echo [1/3] Building JAR with Maven (release profile)...
echo.
call mvn clean package -Prelease -DskipTests

if %ERRORLEVEL% neq 0 (
    echo.
    echo =====================================================
    echo   BUILD FAILED - Check the errors above.
    echo =====================================================
    pause
    exit /b 1
)

REM --- Step 2: Remove levels above MAX_LEVELS from JAR ---
echo.
echo [2/3] Removing levels above %MAX_LEVELS% from JAR...

REM Build a regex pattern for levels to remove
SET "REMOVE_PATTERN="
SET "LEVELS_REMOVED="

if %MAX_LEVELS% LSS 5 (
    SET "REMOVE_PATTERN=Level5Config|level5"
    SET "LEVELS_REMOVED=5"
)
if %MAX_LEVELS% LSS 4 (
    SET "REMOVE_PATTERN=!REMOVE_PATTERN!|Level4Config|level4"
    SET "LEVELS_REMOVED=4, 5"
)
if %MAX_LEVELS% LSS 3 (
    SET "REMOVE_PATTERN=!REMOVE_PATTERN!|Level3Config|level3"
    SET "LEVELS_REMOVED=3, 4, 5"
)
if %MAX_LEVELS% LSS 2 (
    SET "REMOVE_PATTERN=!REMOVE_PATTERN!|Level2Config|level2"
    SET "LEVELS_REMOVED=2, 3, 4, 5"
)

if defined REMOVE_PATTERN (
    echo   Removing levels: %LEVELS_REMOVED%
    powershell -Command ^
        "Add-Type -AssemblyName System.IO.Compression.FileSystem;" ^
        "$jar = [System.IO.Compression.ZipFile]::Open('%JAR_FILE%', 'Update');" ^
        "$toRemove = @($jar.Entries | Where-Object { $_.FullName -match '%REMOVE_PATTERN%' });" ^
        "foreach ($entry in $toRemove) { $entry.Delete() };" ^
        "Write-Host ('  Removed ' + $toRemove.Count + ' entries from JAR');" ^
        "$jar.Dispose()"

    if %ERRORLEVEL% neq 0 (
        echo   WARNING: Failed to trim levels from JAR.
    )
) else (
    echo   All 5 levels included - nothing to remove.
)

REM --- Step 3: Copy save files for distribution ---
echo.
echo [3/3] Packaging save files...

if not exist target\saves mkdir target\saves
if exist saves\*.sav copy saves\*.sav target\saves\ >nul 2>&1
if exist saves\.profiles.json copy saves\.profiles.json target\saves\ >nul 2>&1
echo   Save files copied to target\saves

REM --- Done ---
echo.
echo =====================================================
echo   BUILD SUCCESSFUL
echo =====================================================
echo.
echo   Release JAR: %JAR_FILE%
echo   Levels:      1 to %MAX_LEVELS%
echo.
echo   This JAR is ready for distribution to players.
echo =====================================================
echo.

endlocal
pause
