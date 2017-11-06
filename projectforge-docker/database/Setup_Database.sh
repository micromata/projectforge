#!/bin/bash

if [ "$#" -ne 5 ]; then
    echo "Usage: $0 [container name] [docker file] [host port] [persistent storage] [Postgres version]"
    exit
fi

containerName="$1"
dockerFile="$2"
hostPort="$3"
storagePath="$4"
postgresVersion="$5"

# pull postgres
docker pull postgres:$postgresVersion

# create image
echo "build image postgres"
docker build --rm=true .

# stop container
echo "stop $containerName"
docker stop $containerName

# delete container
echo "remove $containerName"
docker rm $containerName

# run container
echo "run container $containerName"
docker run -d  --name $containerName \
	-p $hostPort:5432 \
	-v "$(pwd)"/docker-entrypoint-initdb.d:/docker-entrypoint-initdb.d \
	-v /etc/localtime:/etc/localtime:ro \
	-v $storagePath:/var/lib/postgresql/data \
	postgres
