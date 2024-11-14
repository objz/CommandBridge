#!/bin/bash

# Server paths
SERVER1_DIR="/mnt/FastStorage/Server-TEST/SocketLib/Velocity/"
SERVER2_DIR="/mnt/FastStorage/Server-TEST/SocketLib/Bukkit/"

# Server JAR files
SERVER1_JAR="velocity-3.3.0-SNAPSHOT-436.jar"
SERVER2_JAR="paper-1.21.1-99.jar"

# Function to run a server
run_server() {
  local server_dir=$1
  local server_jar=$2
  local extra_args=$3
  cd "$server_dir" || exit
  java -Xmx1024M -Xms1024M -jar "$server_jar" $extra_args
}

# Start the first server in the current terminal
echo "Starting Velocity server in the current terminal..."
run_server "$SERVER1_DIR" "$SERVER1_JAR" &

# Start the second server in a new Kitty terminal with nogui flag
echo "Starting Bukkit server in a new Kitty terminal..."
kitty bash -c "cd '$SERVER2_DIR' && java -Xmx1024M -Xms1024M -jar '$SERVER2_JAR' nogui; exec bash"

# Wait for the first server to complete
wait
