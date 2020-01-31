+++ HINWEIS (Aktuelle Postgres Version 10.0)+++

### Um den Postgres Datanbank Container zu bauen, müssen folgende Schritte ausgeführt werden:

##### Auf dem Mac muss das Verzeichnis database/docker-entrypoint-initdb mountbar sein.

./startDatabase.sh postgres 15432

### Um die Datenbank zu verwerfen Docker Container entfernen und docker Volume löschen

./resetDatabase.sh

### Um die Docker Volume Infos anzuzeigen

docker volume inspect database_db-data
