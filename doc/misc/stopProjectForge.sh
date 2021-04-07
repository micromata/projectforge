#!/bin/bash

pid=$(pgrep -f "java.*-jar projectforge-application")
if [[ -z $pid ]]; then
    echo "ProjectForge process not found"
    exit 0
else
    kill $pid
fi

echo "waiting 10 sec for termination of pid $pid..."
sleep 10

pid=$(pgrep -f "java.*-jar projectforge-application")
if [[ -z $pid ]]; then
    echo "ProjectForge stopped"
    exit 0
else
    echo "ProjectForge not stopped, now sending sigkill"
    kill -9 $pid
fi

sleep 0.5

pid=$(pgrep -f "java.*-jar projectforge-application")
if [[ -z $pid ]]; then
    echo "ProjectForge killed"
    exit 0
else
    echo "ProjectForge could not be killed"
    exit 1
fi
