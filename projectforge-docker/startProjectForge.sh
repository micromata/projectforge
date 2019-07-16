#!/bin/bash

databaseType="hsqldb"
custom=""

for var in "$@"
do
  if [ "$var" == "custom" ]; then
    custom="custom"
  fi
done

while getopts ":d:" opt; do
  case $opt in
    d)
      echo "-db was triggered, Parameter: $OPTARG"
      databaseType=$OPTARG
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

docker info > /dev/null || exit 1

if [ "$databaseType" == "hsqldb" ]; then
  startProjectForgeHSQLDB.sh ${custom}
fi

if [ "$databaseType" == "postgres" ]; then
  startProjectForgePostgres.sh ${custom}
fi
