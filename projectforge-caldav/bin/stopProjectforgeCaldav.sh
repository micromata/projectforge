#!/bin/bash

pid=$(pgrep -f "java.*-jar projectforge-caldav")
if [[ -z $pid ]]; then
    echo "ProjectForge CalDAV process not found"
    exit 0
else
    kill $pid
fi

echo "waiting 2 sec for termination of pid $pid..."
sleep 2

pid=$(pgrep -f "java.*-jar projectforge-caldav")
if [[ -z $pid ]]; then
    echo "ProjectForge CalDAV stopped"
    exit 0
else
    echo "ProjectForge CalDAV not stopped, now sending sigkill"
    kill -9 $pid
fi

sleep 0.5

pid=$(pgrep -f "java.*-jar projectforge-caldav")
if [[ -z $pid ]]; then
    echo "ProjectForge CalDAV killed"
    exit 0
else
    echo "ProjectForge CalDAV could not be killed"
    exit 1
fi
