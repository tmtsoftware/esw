#!/usr/bin/env bash

NOCOLOR='\033[0m'  # No Color
Green='\033[0;32m' # Green

function log() {
  echo -e "${Green}$1${NOCOLOR}"
}

# Run from the directory containing the script
cd "$(dirname "${BASH_SOURCE[0]}")" || exit

# Publish scala and kotlin artifacts locally to .m2
./build.sh

log "sbt > Running sbt tests..."
sbt test

log "gradle > Running kotlin tests..."
./gradle.sh test

log "sbt > Running integration tests..."
sbt esw-integration-test/test
