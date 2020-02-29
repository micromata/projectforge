#!/bin/bash

echo "Make sure, that you've run 'mvn clean install' on top directory first"

echo "Unpacking spring boot fat jar to target/dependency..."
rm -rf target/dependency
mkdir -p target/dependency && (cd target/dependency; jar -xf ../../../projectforge-application/target/*.jar)

echo "Building docker file..."
docker build -t micromata/projectforge .

echo "Run with 'docker run -p 8080:8080 -v ~/ProjectForge:/home/projectforge/ProjectForge micromata/projectforge'"
