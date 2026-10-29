@echo off
setlocal

echo =============================================
echo   Courier ^& Parcel Tracking System - Build
echo =============================================

REM Create output directories
if not exist "out\classes" mkdir "out\classes"

REM Collect all Java source files
for /r "src\main\java" %%f in (*.java) do (
    echo %%f >> sources.txt
)

echo Compiling Java sources...
javac -cp "lib\sqlite-jdbc.jar;lib\slf4j-api.jar;lib\slf4j-nop.jar" -d "out\classes" @sources.txt
del sources.txt

if errorlevel 1 (
    echo [ERROR] Compilation failed!
    pause
    exit /b 1
)

echo Creating manifest...
echo Main-Class: com.courier.Main> MANIFEST.MF
echo Class-Path: lib/sqlite-jdbc.jar lib/slf4j-api.jar lib/slf4j-nop.jar>> MANIFEST.MF

echo Packaging JAR...
jar cfm CourierTrackingSystem.jar MANIFEST.MF -C "out\classes" .
del MANIFEST.MF

echo.
echo [SUCCESS] Build complete: CourierTrackingSystem.jar
echo.
echo To run: java -cp "CourierTrackingSystem.jar;lib\sqlite-jdbc.jar" com.courier.Main
pause
