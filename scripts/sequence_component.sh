#!/usr/bin/env bash

SCRIPT_PATH="$(
    cd "$(dirname "$0")" >/dev/null 2>&1 || exit
    pwd -P
)"

"$SCRIPT_PATH"/ocs_app.sh seqcomp "$@"