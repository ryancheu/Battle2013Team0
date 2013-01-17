#!/bin/bash

# Change this to the directory containing your Battlecode installation
cd /home/garywang/Projects/Battlecode
ant -Dmatch=$1 runmatch
