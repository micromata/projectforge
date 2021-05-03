#!/bin/bash

JAVA_MAIN="projectforge-application"
APP_NAME="ProjectForge"

checkStopped() {
  pid=$(pgrep -f $JAVA_MAIN)
  if [[ -z $pid ]]; then
    echo "${APP_NAME} $1"
    exit 0
  fi
}

checkStopped "process not found (already terminated?)."

echo "Sending shutdown signal to $APP_NAME..."
kill $pid

# Loop 10 times a 3 seconds to wait for ProjectForge's shutdown:
for run in {1..20}; do
  echo "waiting 3 sec for termination of pid $pid..."
  sleep 3
  checkStopped "successfully stopped."
done

echo "${APP_NAME} not stopped, sending sigkill now..."
kill -9 $pid

sleep 2

checkStopped "killed."
