# ProjectForge Docker
#### Run with HSQLDB volume mount

.StartProjectforge.sh

#### Run with Postgres DB

.StartProjectforge -db postgres

Connection from Host: Localhost / 15432 / projectforge / projectforge

#### Run with custom application.properties
Edit ./customConfig/application-default.properties

.StartProjectforge.sh (-d postgres) custom

#### Connect to docker maschines

docker exec -ti projectforge-app
docker exec -ti projectforge-db

##### NOTES

- To clear HSQL DB -> Delete ./Projectforge/database/projectforge
- To clear Postgres DB -> Run ./database/ResetDatabase.sh
- ./Projectforge ist PF base directory, if not configured other