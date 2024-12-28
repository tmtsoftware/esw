#!/usr/bin/env bash

FILE_PATH=""
CLASSNAME=""
SUBSYSTEM=""
OBS_MODE=""
VERSION=""

function usage() {
  echo
  echo -e "usage: $script_name COMMAND [--filePath | -f <script file>] [--subsystem | -s <Subsystem>] [--obsMode | -o <Observing mode>] [--script-version | -v <version of scripts repo>]"

  echo "  --filePath | -f <script file>               Path of the script"
  echo "  --subsystem | -s <Subsystem>                (Optional) Subsystem of the Sequencer. Default value: ESW"
  echo "  --obsMode | -o <Observing mode>             (Optional) Observing mode of the Sequencer. Default value: <class name of the script>"
  echo "  --script-version | -v <version of scripts repo>    (Optional) Version of Sequencer-Scripts repo with which script is compatible."
}

function parseArgs() {
  while [[ $# -gt 1 ]]; do
    key="$1"

    case ${key} in
    --filePath | -f)
      FILE_PATH=$2
      filename=${FILE_PATH##*/}
      CLASSNAME=${filename%.*}
      CLASSNAME="$(tr '[:lower:]' '[:upper:]' <<<${CLASSNAME:0:1})${CLASSNAME:1}"
      ;;

    --subsystem | -s)
      SUBSYSTEM=$2
      ;;

    --obsMode | -o)
      OBS_MODE=$2
      ;;

    --script-version | -v)
      VERSION=":${2}"
      ;;

    *)
      echo "[ERROR] Unknown arguments provided for start command. Find usage below:"
      usage
      ;;
    esac
    shift
    shift
  done

  if [[ ${FILE_PATH} == "" ]]; then
    echo "[ERROR] Script path cannot be empty"
    usage
    exit 1
  fi
}

parseArgs "$@"

# Add default subsystem if not provided
if [[ ${SUBSYSTEM} == "" ]]; then
  SUBSYSTEM="ESW"
fi

# Add default obsMode if not provided
if [[ ${OBS_MODE} == "" ]]; then
  OBS_MODE="${CLASSNAME}"
fi
# check if channel is available in env (used in test)
if [[ ${CS_CHANNEL} == "" ]]; then
#  CS_CHANNEL="https://raw.githubusercontent.com/tmtsoftware/osw-apps/master/apps.json"
  CS_CHANNEL="https://raw.githubusercontent.com/tmtsoftware/osw-apps/Allan/pekko-scala3-update/apps.json"
fi

# ---------------- Compiling ------------------
echo "[INFO] Compiling the script:" $FILE_PATH
JARNAME=$CLASSNAME.jar

kotlinc -Xuse-fir-lt=false -jvm-target 17 -opt-in=kotlin.time.ExperimentalTime -Xallow-any-scripts-in-source-roots -classpath "$(cs fetch --channel $CS_CHANNEL esw-ocs-app$VERSION --classpath)" $FILE_PATH -d $JARNAME

if [[ $? -eq 1 ]]; then
  echo "[ERROR] Compilation failed. Fix the compiler errors and also Make sure script is .kts file"
  exit 1
fi
echo "[INFO] Compilation completed. Compiled jar name:" $JARNAME

# ---------------- Launching sequencer ------------------
echo "[INFO] Launching sequencer with Subsystem:" $SUBSYSTEM "and Observation Mode:" $OBS_MODE
cs launch --channel $CS_CHANNEL --extra-jars $JARNAME --java-opt -Dscripts.$SUBSYSTEM.$OBS_MODE.scriptClass="$CLASSNAME" esw-ocs-app$VERSION -- sequencer -s $SUBSYSTEM -m $OBS_MODE
