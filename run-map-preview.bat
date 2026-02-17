@echo off
REM ==================================================
REM Map Preview Tool Launcher
REM ==================================================
REM This script compiles the project and runs the
REM MapPreviewTool separately from the main game
REM for level design purposes.
REM ==================================================

echo Building project...
call mvn compile -q
if %ERRORLEVEL% neq 0 (
    echo BUILD FAILED - fix compile errors first.
    pause
    exit /b 1
)

echo Starting Map Preview Tool...

REM Build classpath: compiled classes + resources + all Maven dependencies
for /f "delims=" %%i in ('mvn dependency:build-classpath -q -DincludeScope=runtime -Dmdep.outputFile=CON') do set DEPS=%%i
set CLASSPATH=target/classes;res;%DEPS%

REM Run the MapPreviewTool
java -cp "%CLASSPATH%" com.buglife.tools.MapPreviewTool

pause
