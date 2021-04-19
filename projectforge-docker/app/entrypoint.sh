#!/bin/bash

# https://stackoverflow.com/questions/41451159/how-to-execute-a-script-when-i-terminate-a-docker-container

JAVA_MAIN="org.projectforge.start.ProjectForgeApplication"
APP_NAME="ProjectForge"

#Define cleanup procedure
cleanup() {
  echo "Container stopped, performing cleanup..."

  pid=$(pgrep -f $JAVA_MAIN)
  if [[ -z $pid ]]; then
    echo "${APP_NAME} process not found."
    exit 0
  else
    echo "Sending shutdown signal to $APP_NAME..."
    kill $pid
  fi

  echo "waiting 10 sec for termination of pid $pid..."
  sleep 10

  pid=$(pgrep -f $JAVA_MAIN)
  if [[ -z $pid ]]; then
    echo "${APP_NAME} stopped"
    exit 0
  else
    echo "${APP_NAME} not stopped, now sending sigkill"
    kill -9 $pid
  fi

  sleep 0.5

  pid=$(pgrep -f $JAVA_MAIN)
  if [[ -z $pid ]]; then
    echo "${APP_NAME} killed"
    exit 0
  else
    echo "${APP_NAME} could not be killed"
    exit 1
  fi
}

echo "Starting ${APP_NAME}..."

ENVIRONMENT_FILE=/ProjectForge/environment.sh
if [ -f "$ENVIRONMENT_FILE" ]; then
  echo "Sourcing $ENVIRONMENT_FILE..."
  . $ENVIRONMENT_FILE
fi

if [ -n "$JAVA_OPTS" ]; then
  echo "JAVA_OPTS=${JAVA_OPTS}"
fi

if [ -n "$JAVA_ARGS" ]; then
  echo "JAVA_ARGS=${JAVA_ARGS}"
fi

#Trap SIGTERM
trap cleanup INT SIGTERM

START="${JAVA_OPTS} -cp app:app/lib/*:/ProjectForge/plugins/* -Dprojectforge.setup=docker -Dprojectforge.plugins.dir=/ProjectForge/plugins ${JAVA_MAIN} ${JAVA_ARGS}"
# (projectforge.setup is defined in ProjectForgeApp.)

echo "Starting: java ${START}"

CONFIG_FILE=/ProjectForge/projectforge.properties
if [ -f "$CONFIG_FILE" ]; then
  # Initial start: java must run in background for getting the SIGTERM signal on stop (wait $CHILD).
  echo "Normal start"
  java $START &
else
  # Initial start: java must run in foreground for using the console setup wizard.
  echo "Initial start"
  java $START
fi

CHILD=$!
wait $CHILD

echo "$APP_NAME terminated."
#wait $!

#Cleanup
#cleanup Not needed, Java process already terminated.
