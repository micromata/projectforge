#!/bin/bash

PROCESS_IDENTIFIER="java.*projectforge-application"
APP_NAME="ProjectForge"

checkStopped() {
  pid=$(pgrep -f $PROCESS_IDENTIFIER)
  if [[ -z $pid ]]; then
    echo "${APP_NAME} $1"
    exit 0
  fi
  if [[ -n $2 ]]; then
    echo "${APP_NAME} $2"
  fi
}

checkStopped "process not found (already terminated?)"

echo "Sending shutdown signal to $APP_NAME..."
kill $pid

# Loop 20 times a 3 seconds to wait for ProjectForge's shutdown:
for run in {1..20}; do
  echo "waiting 3 sec for termination of pid $pid..."
  sleep 3
  checkStopped "successfully stopped."
done

checkStopped "successfully stopped." "not stopped, sending sigkill now..."
kill -9 $pid

sleep 2

checkStopped "killed." "cannot be killed?!"
