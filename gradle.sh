#!/usr/bin/env bash

# Run from the directory containing the script
cd "$(dirname "${BASH_SOURCE[0]}")" || exit

./esw-kt/gradlew -p esw-kt "$@"