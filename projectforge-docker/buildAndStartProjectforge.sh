#!/bin/bash
docker info > /dev/null || exit 1

cd database
./StartDatabase.sh 15432

cd ../../
mvn clean install -DskipTests=true

cd projectforge-docker

# stop container
echo "stop projectforge-app"
docker stop projectforge-app

# delete container
echo "remove projectforge-app"
docker rm projectforge-app

docker create -p 127.0.0.1:8080:8080 -v $PWD/Projectforge:/home/pf/Projectforge --name projectforge-app --network=database_projectforge-network micromata/projectforge:6.19.0-SNAPSHOT

if [ "$1" == "postgres" ]; then
  echo "Using postgres db for app configuration"
  docker cp ./config/application-default.properties projectforge-app:/home/pf/application-default.properties
fi

docker start projectforge-app