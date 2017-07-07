# ProjectForge Docker
#### Run with HSQLDB volume mount
`docker run --rm -ti -p 127.0.0.1:8080:8080 -v $PWD:/home/pf/config micromata/projectforge:6.15.0-SNAPSHOT`

#### Use own application.properties
Place application.properties in current folder and run:
`docker run --rm -ti -p 127.0.0.1:8080:8080 -v $PWD:/home/pf/config micromata/projectforge:6.15.0-SNAPSHOT`
