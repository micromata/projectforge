#!/bin/bash
set -e

echo
echo 'Creating custom user and database...'
echo

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
    CREATE USER projectforge WITH PASSWORD 'projectforge';
    CREATE DATABASE projectforge;
    GRANT ALL PRIVILEGES ON DATABASE projectforge TO projectforge;
EOSQL
