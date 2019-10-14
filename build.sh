#!/usr/bin/env bash

NOCOLOR='\033[0m'  # No Color
Green='\033[0;32m' # Green

function log() {
  echo -e "${Green}$1${NOCOLOR}"
}

# Run from the directory containing the script
cd "$(dirname "${BASH_SOURCE[0]}")" || exit

# publish esw scala artifacts to local m2 (this does not include esw-integration-test)
log "sbt > Publishing esw scala artifacts..."
sbt esw-integration-test/clean
sbt clean publishM2

# publish kotlin artifacts to local m2
log "gradle > Publishing kotlin artifacts..."
./gradle.sh clean publishToMavenLocal
