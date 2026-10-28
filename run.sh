#!/usr/bin/env bash
set -e

echo "======================================================================="
echo "         DSA-Powered Banking CLI Application - Launcher"
echo "======================================================================="

if ! command -v javac &> /dev/null; then
    echo "[ERROR] 'javac' (JDK) was not found in your PATH."
    echo "Please install OpenJDK 17+ and add it to your PATH."
    exit 1
fi

mkdir -p out

echo "Compiling Java source files..."
find src/main/java -name "*.java" > sources.txt
javac -d out @sources.txt
rm sources.txt

echo "Launching Console Application..."
echo ""
java -cp out com.bank.cli.ConsoleBankApp
