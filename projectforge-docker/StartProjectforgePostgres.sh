#!/bin/bash
docker info > /dev/null || exit 1

cd database
./StartDatabase.sh 15432 postgres

cd ../../
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

cd customConfig

if grep -q 'spring.datasource.url' "application-default.properties"; then
   sed -i '' -e 's|.*spring.datasource.url.*|spring.datasource.url=jdbc:postgresql://projectforge-db:5432/projectforge|' application-default.properties
else
   echo "spring.datasource.url=jdbc:postgresql://projectforge-db:5432/projectforge" >> application-default.properties
fi

if grep -q 'spring.datasource.username' "application-default.properties"; then
   sed -i '' -e 's|.*spring.datasource.username.*|spring.datasource.username=projectforge|' application-default.properties
else
   echo "spring.datasource.username=projectforge" >> application-default.properties
fi

if grep -q 'spring.datasource.password' "application-default.properties"; then
   sed -i '' -e 's|.*spring.datasource.password.*|spring.datasource.password=projectforge|' application-default.properties
else
   echo "spring.datasource.password=projectforge" >> application-default.properties
fi

if grep -q 'spring.datasource.driver-class-name' "application-default.properties"; then
   sed -i '' -e 's|.*spring.datasource.driver-class-name.*|spring.datasource.driver-class-name=org.postgresql.Driver|' application-default.properties
else
   echo "spring.datasource.driver-class-name=org.postgresql.Driver" >> application-default.properties
fi

docker cp ./application-default.properties projectforge-app:/home/pf/application-default.properties

docker start projectforge-app