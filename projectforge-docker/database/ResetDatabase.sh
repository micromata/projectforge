#!/bin/bash
docker info > /dev/null || exit 1

docker stop projectforge-db

docker rm projectforge-db

docker volume rm database_db-data

echo "Finished resetting projectforge database"