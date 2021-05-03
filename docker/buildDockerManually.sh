#!/bin/bash

# For building docker image, if application was built before (mvn clean install).

echo "Make sure, that you've run 'mvn clean install' on top directory first"

echo "Unpacking spring boot fat jar to target/dependency..."
rm -rf dependencies
mkdir -p dependencies && (cd dependencies; jar -xf ../../projectforge-application/target/projectforge-application*.jar)

echo "Building docker file..."
docker build -f Dockerfile-manually -t micromata/projectforge .

rm -rf dependencies

echo "docker tag ..... version"
echo "docker push micromata/projectforge:version"
echo "docker push micromata/projectforge:latest"
echo
echo
echo "Run with 'docker run -t -i -p 127.0.0.1:8080:8080 -v $HOME/ProjectForge:/ProjectForge --name projectforge micromata/projectforge'"

# docker run --name projectforge-postgres -t -i -p 127.0.0.1:15432:5432 -e POSTGRES_PASSWORD=$PGPASSWORD -e POSTGRES_USER=projectforge -d postgres:11.2
