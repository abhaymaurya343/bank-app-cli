@echo off
setlocal enabledelayedexpansion
title Enterprise DSA Banking System (CLI)

echo =======================================================================
echo          DSA-Powered Banking CLI Application - Launcher
echo =======================================================================

REM Check if javac is installed
where javac >nul 2>nul
if %errorlevel% neq 0 (
    echo [INFO] 'javac' (JDK) was not found in your PATH.
    echo [INFO] Launching using native Python CLI engine (run.py)...
    echo.
    python run.py
    if !errorlevel! neq 0 (
        py run.py
    )
    goto end
)

REM Create output directory
if not exist "out" mkdir out

echo Compiling Java source files...
dir /s /b src\main\java\*.java > sources.txt
javac -d out @sources.txt
del sources.txt

if %errorlevel% neq 0 (
    echo [ERROR] Java compilation failed.
    echo [INFO] Falling back to Python CLI engine...
    python run.py
    goto end
)

echo Launching Console Application...
echo.
java -cp out com.bank.cli.ConsoleBankApp

:end
echo.
pause
