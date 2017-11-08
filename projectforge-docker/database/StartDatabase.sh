#!/bin/bash

docker info > /dev/null || exit 1

if [ "$#" -ne 2 ]; then
    echo "Usage: $0 [database type] [host port]"
    exit
fi

# Create the projectforge network, if it is not already created
projectforge_network_created=$( docker network ls -f name=projectforge_network -q )
if [ -z $projectforge_network_created ]
then
    docker network create projectforge_network
fi

hostPort="$1"
databaseType="$2"

if [ "$databaseType" == "postgres" ]; then
  echo "Using postgres db"
  cd postgres
  echo 'HOST_PORT='$hostPort > .env
  docker-compose up -d
fi

