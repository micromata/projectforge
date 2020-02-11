#!/bin/bash

PF_JAR=projectforge-caldav*.jar
PF_HOME_DIR=${HOME}/ProjectForge

while getopts ":v:" opt; do
  case $opt in
    v)
      echo "-v was triggered, Parameter: $OPTARG"
      version=$OPTARG
      ;;
    \?)
      echo "Invalid option: -$OPTARG" >&2
      exit 1
      ;;
    :)
      echo "Option -$OPTARG requires an argument." >&2
      exit 1
      ;;
  esac
done

# go to the directory of this script
cd "$(dirname "$0")"

#DEBUGOPTS=-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=6006
DEBUGOPTS=

# Milton license files should be placed here (milton.license.properties, milton.license.sig)
export LOADER_PATH=${PF_HOME_DIR}/resources/caldav

echo "Starting ProjectForge CalDAV"
java ${DEBUGOPTS} -Dprojectforge.base.dir=${PF_HOME_DIR} -jar $PF_JAR
#nohup java ${DEBUGOPTS} -Dprojectforge.base.dir=${PF_HOME_DIR} -jar $PF_JAR 2>&1 > /dev/null &
