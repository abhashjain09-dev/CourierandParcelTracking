@echo off
title Courier and Parcel Tracking System
echo ========================================================
echo    Courier and Parcel Tracking System (Java Swing + SQLite)
echo ========================================================
echo.

:: 1. If Maven is available, use it directly
where mvn >nul 2>nul
if %errorlevel% equ 0 (
    echo [INFO] Apache Maven detected. Compiling and launching via Maven...
    echo.
    call mvn clean compile exec:java -Dexec.mainClass="com.courier.Main"
    if %errorlevel% neq 0 (
        echo.
        echo [ERROR] Application failed to launch via Maven.
        pause
    )
    exit /b 0
)

:: 2. Fallback to PowerShell runner (which handles standalone compile & dependencies)
echo [INFO] Maven not in PATH. Running standalone launcher...
echo.
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0run.ps1"

if %errorlevel% neq 0 (
    echo.
    echo [NOTE] If you have an IDE installed (IntelliJ IDEA, Eclipse, NetBeans),
    echo        you can simply open this project folder in the IDE and run Main.java!
    echo.
    pause
)
