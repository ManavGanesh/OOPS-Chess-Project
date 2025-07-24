#!/bin/bash

echo "Starting Chess Server..."
echo "Make sure you're in the project directory: Chess-main/ChessProject"

# Navigate to the src directory and compile the server classes
cd src/main/java

# Compile the server classes
echo "Compiling server classes..."
javac -cp . server/*.java Messages/*.java chess_game/Pieces/*.java

# Start the server
echo "Starting server on port 4000..."
java -cp . server.Start

echo "Server stopped."
