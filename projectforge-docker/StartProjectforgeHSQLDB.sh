#!/bin/bash
docker info > /dev/null || exit 1

# Create the projectforge network, if it is not already created
projectforge_network_created=$( docker network ls -f name=projectforge_network -q )
if [ -z $projectforge_network_created ]
then
    docker network create projectforge_network
fi

cd ../
mvn clean install -DskipTests=true

cd projectforge-docker

PF_VERSION=$(grep --max-count=1 '<version>' pom.xml | awk -F '>' '{ print $2 }' | awk -F '<' '{ print $1 }')

# stop container
echo "stop projectforge-app"
docker stop projectforge-app

# delete container
echo "remove projectforge-app"
docker rm projectforge-app

docker create -p 127.0.0.1:8080:8080 -v $PWD/Projectforge:/home/pf/Projectforge --name projectforge-app --network=projectforge_network micromata/projectforge:${PF_VERSION}

if [ "$1" == "custom" ]; then
  echo "Using customized app configuration"
  docker cp ./customConfig/application-default.properties projectforge-app:/home/pf/application-default.properties
fi

docker start projectforge-app