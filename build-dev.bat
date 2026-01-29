@echo off
REM =========================================
REM Build DEVELOPMENT version (for devs)
REM Includes all tools
REM =========================================

echo Building Development Version...
echo This build INCLUDES all developer tools.
echo.

mvn clean package -Pdev -DskipTests

if %ERRORLEVEL% equ 0 (
    echo.
    echo ========================================
    echo BUILD SUCCESSFUL!
    echo ========================================
    echo Dev JAR: target\lullaby-down-below-1.5.0-jar-with-dependencies.jar
    echo.
    echo Use run-level-editor.bat to launch the Level Editor.
    echo.
) else (
    echo.
    echo BUILD FAILED! Check errors above.
)
pause
