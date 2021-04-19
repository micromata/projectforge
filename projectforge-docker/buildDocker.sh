#!/bin/bash

echo "Make sure, that you've run 'mvn clean install' on top directory first"

echo "Unpacking spring boot fat jar to target/dependency..."
rm -rf app/target/dependency
mkdir -p app/target/dependency && (cd app/target/dependency; jar -xf ../../../../projectforge-application/target/projectforge-application*.jar)

echo "Building docker file..."
(cd app; docker build -t kreinhard/projectforge .)

echo "docker tag ..... version"
echo "docker push kreinhard/projectforge:version"
echo "docker push kreinhard/projectforge:latest"
echo
echo
echo "Run with 'docker run -t -i -p 127.0.0.1:8080:8080 -v $HOME/ProjectForge:/ProjectForge --name projectforge kreinhard/projectforge'"

# docker run --name projectforge-postgres -t -i -p 127.0.0.1:15432:5432 -e POSTGRES_PASSWORD=$PGPASSWORD -e POSTGRES_USER=projectforge -d postgres:11.2
