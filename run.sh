#!/bin/bash
# run.sh
# Compile all Java files first
javac *.java

# Run the application with different node IDs
java App node01 &
java App node02 &
java App node03 &
java App node04 &