#!/bin/bash

# Chess System Cleanup Utility Script
# This script provides easy access to clean up user registry and saved games

cd "$(dirname "$0")"

echo "🧹 Chess System Cleanup Utility"
echo "================================"

# Check if Java files are compiled
if [ ! -f "src/main/java/server/SystemCleanup.class" ] || [ ! -f "src/main/java/server/QuickCleanup.class" ]; then
    echo "📦 Compiling cleanup utilities..."
    javac -cp "lib/*:src/main/java" src/main/java/server/SystemCleanup.java src/main/java/server/QuickCleanup.java
    
    if [ $? -ne 0 ]; then
        echo "❌ Compilation failed. Please check for errors."
        exit 1
    fi
    echo "✅ Compilation successful!"
    echo
fi

# Run the cleanup utility
# Check for command line arguments
if [ $# -gt 0 ]; then
    echo "🚀 Running quick cleanup with args: $@"
    echo
    java -cp "lib/*:src/main/java" server.QuickCleanup "$@"
else
    echo "🚀 Starting interactive cleanup utility..."
    echo
    java -cp "lib/*:src/main/java" server.SystemCleanup
fi

echo
echo "🏁 Cleanup utility finished."
