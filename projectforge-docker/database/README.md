+++ HINWEIS (Aktuelle Postgres Version 10.0)+++

Um den Postgres Datanbank Container zu bauen, müssen folgende Schritte ausgeführt werden:

# in den Ordner "database" wechseln und dort einen Datenbankcontainer bauen
# Auf dem Mac muss das Verzeichnis database/docker-entrypoint-initdb mountbar sein.

cd database
./Setup_Database.sh projectforge-db Dockerfile 127.0.0.1:15432 $(pwd)/data/projectforge/db 10.0