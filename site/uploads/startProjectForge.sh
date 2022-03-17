#!/bin/bash

PF_JAR=${HOME}/application/projectforge-application-xxx.jar"

echo "Using ProjectForge jar: ${PF_JAR}..."

if [ "${OSTYPE}" == 'cygwin' ]
then
    JAVA=`cygpath "${JAVA_HOME}"`/jre/bin/java
else
    if [[ -n "$JAVA_HOME" ]] && [[ -x "$JAVA_HOME/bin/java" ]];  then
    	JAVA="$JAVA_HOME/bin/java"
	else
    	JAVA=/usr/bin/java
	fi
fi

echo "Using ${JAVA}"

DEBUGOPTS=

nohup ${JAVA}  -Xms4g -Xmx4g ${DEBUGOPTS} -jar $PF_JAR 2>&1 > /dev/null &
