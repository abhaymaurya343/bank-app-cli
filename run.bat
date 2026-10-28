@echo off
setlocal enabledelayedexpansion

echo =======================================================================
echo          DSA-Powered Banking CLI Application - Launcher
echo =======================================================================

REM Check if javac is installed
where javac >nul 2>nul
if %errorlevel% neq 0 (
    echo [ERROR] 'javac' (Java Development Kit) was not found in your PATH.
    echo Please install JDK 17+ and ensure JAVA_HOME is configured.
    pause
    exit /b 1
)

REM Create output directory
if not exist "out" mkdir out

echo Compiling Java source files...
dir /s /b src\main\java\*.java > sources.txt
javac -d out @sources.txt
del sources.txt

if %errorlevel% neq 0 (
    echo [ERROR] Compilation failed.
    pause
    exit /b 1
)

echo Launching Console Application...
echo.
java -cp out com.bank.cli.ConsoleBankApp

pause
