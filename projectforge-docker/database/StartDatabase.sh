#!/bin/bash
docker info > /dev/null || exit 1

if [ "$#" -ne 1 ]; then
    echo "Usage: $0 [host port]"
    exit
fi

hostPort="$1"

echo 'HOST_PORT='$hostPort > .env

docker-compose up -d