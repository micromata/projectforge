# ProjectForge Docker
#### Run with HSQLDB volume mount

.startProjectForge.sh

#### Run with Postgres DB

.startProjectForge -db postgres

Connection from Host: Localhost / 15432 / projectforge / projectforge

#### Run with custom application.properties
Edit ./customConfig/application-default.properties

.startProjectForge.sh (-d postgres) custom

#### Connect to docker maschines

docker exec -ti projectforge-app
docker exec -ti projectforge-db

##### NOTES

- To clear HSQL DB -> Delete ./ProjectForge/database/projectforge
- To clear Postgres DB -> Run ./database/resetDatabase.sh
- ./ProjectForge ist PF base directory, if not configured other
