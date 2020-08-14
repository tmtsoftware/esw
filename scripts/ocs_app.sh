#!/usr/bin/env bash

SCRIPT_PATH="$(
    cd "$(dirname "$0")" >/dev/null 2>&1 || exit
    pwd -P
)"
SCRIPTS_VERSION=""
COURSIER="$(command -v cs)" || COURSIER="$SCRIPT_PATH/coursier"
APPS_PATH="https://raw.githubusercontent.com/tmtsoftware/osw-apps/master/apps.json"
APP_NAME="ocs-app"

# capture version number and store rest of the arguments to arr variable which then passed to cs launch
while (("$#")); do
    case "$1" in
        -v) SCRIPTS_VERSION="$2"; shift; shift;;
        *) arr+=("$1"); shift;;
    esac
done

if [[ -z "${SCRIPTS_VERSION// }" ]]; then
    echo "==== Using scripts version : latest.stable ===="
    "$COURSIER" launch --channel $APPS_PATH "$APP_NAME" -- "${@:1}"
else
    echo "==== Using scripts version : $SCRIPTS_VERSION ===="
    "$COURSIER" launch --channel $APPS_PATH "$APP_NAME":"$SCRIPTS_VERSION" -- "${arr[@]}"
fi
