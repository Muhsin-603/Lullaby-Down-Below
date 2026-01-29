@echo off
REM ==================================================
REM Map Preview Tool Launcher
REM ==================================================
REM This script runs the MapPreviewTool separately 
REM from the main game for level design purposes.
REM ==================================================

echo Starting Map Preview Tool...

REM Set the classpath to include compiled classes and resources
set CLASSPATH=target/classes;res

REM Run the MapPreviewTool
java -cp "%CLASSPATH%" com.buglife.tools.MapPreviewTool

pause
