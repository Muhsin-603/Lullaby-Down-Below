@echo off
REM =========================================
REM Build RELEASE version (for players)
REM Excludes all developer tools
REM =========================================

echo Building Release Version...
echo This build will NOT include:
echo   - Level Editor
echo   - Map Preview Tool
echo   - Debug Exporter
echo   - Performance Monitor
echo.

mvn clean package -Prelease -DskipTests

if %ERRORLEVEL% equ 0 (
    echo.
    echo ========================================
    echo BUILD SUCCESSFUL!
    echo ========================================
    echo Release JAR: target\lullaby-down-below-1.5.0-release.jar
    echo.
    echo This JAR is ready for distribution to players.
    echo.
) else (
    echo.
    echo BUILD FAILED! Check errors above.
)
pause
