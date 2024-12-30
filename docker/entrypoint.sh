#!/bin/bash

# https://stackoverflow.com/questions/41451159/how-to-execute-a-script-when-i-terminate-a-docker-container

APP_NAME="ProjectForge"
JAR_FILE="/app/application.jar"

if ! command -v java &> /dev/null; then
  echo "Error: java is not installed or not in PATH"
  exit 1
fi

checkStopped() {
  pid=$(pgrep -f $JAR_FILE)
  if [[ -z $pid ]]; then
    echo "${APP_NAME} $1"
    exit 0
  fi
  if [[ -n $2 ]]; then
    echo "${APP_NAME} $2"
  fi
}

#Define cleanup procedure
cleanup() {
  echo "Container stopped, performing cleanup..."

  checkStopped "process not found (already terminated?)"

  echo "Sending shutdown signal to $APP_NAME..."
  kill $pid

  # Loop 20 times a 3 seconds to wait for ProjectForge's shutdown:
  for run in {1..20}; do
    echo "waiting 3 sec for termination of pid $pid..."
    sleep 3
    checkStopped "successfully stopped."
  done

  echo "${APP_NAME} not stopped, sending sigkill now..."
  kill -9 $pid

  sleep 2

  checkStopped "killed." "cannot be killed?!"
}

echo "Starting ${APP_NAME}..."

ENVIRONMENT_FILE=/ProjectForge/environment.sh
if [ -f "$ENVIRONMENT_FILE" ]; then
  echo "Sourcing $ENVIRONMENT_FILE..."
  . $ENVIRONMENT_FILE
fi

if [ -n "$JAVA_OPTS" ]; then
  # Print JAVA_OPTS if given:
  echo "JAVA_OPTS=${JAVA_OPTS}"
fi

if [ -n "$JAVA_ARGS" ]; then
  # Print JAVA_ARGS if given:
  echo "JAVA_ARGS=${JAVA_ARGS}"
fi

#Trap SIGTERM
trap cleanup INT SIGTERM

if [ -z "$DOCKER_OPTS" ]; then
  # If no DOCKER_OPTS given, use this as default:
  DOCKER_OPTS="-Ddocker=single"
fi

START="${JAVA_OPTS} ${DOCKER_OPTS} -jar $JAR_FILE ${JAVA_ARGS}"
# (projectforge.setup is defined in ProjectForgeApp.)

echo "Starting: java ${START}"

CONFIG_FILE=/ProjectForge/projectforge.properties
if [ -f "$CONFIG_FILE" ]; then
  # CONFIG_FILE does exist, so assume normal start:
  # java must run in background for getting the SIGTERM signal on stop (wait $CHILD).
  echo "Normal start"
  java $START &
else
  # CONFIG_FILE doesn't exist, so assume initial start:
  # java must run in foreground for using the console setup wizard.
  echo "Initial start"
  java $START
fi

CHILD=$!
wait $CHILD

echo "$APP_NAME terminated."

#Cleanup
#cleanup Not needed, Java process already terminated.
