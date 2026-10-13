#!/bin/bash

# Example start script for ProjectForge without Docker (for later starts, after the setup).
#
# The first start must run in the foreground (java -jar projectforge-application-<version>.jar), so that the
# setup wizard can be shown. This script starts ProjectForge in the background; the log file is written to
# logs/ProjectForge.log in ProjectForge's home directory.
#
# The jar file name must contain "projectforge-application", otherwise stopProjectForge.sh won't find the process.

PF_JAR="${PF_JAR:-${HOME}/application/projectforge-application.jar}"
JAVA_OPTS="${JAVA_OPTS:--Xms2g -Xmx4g}"

if [[ ! -f "${PF_JAR}" ]]; then
  echo "ProjectForge jar not found: ${PF_JAR} (set PF_JAR or edit this script)."
  exit 1
fi

if [[ -n "${JAVA_HOME}" && -x "${JAVA_HOME}/bin/java" ]]; then
  JAVA="${JAVA_HOME}/bin/java"
else
  JAVA=java
fi

echo "Using ProjectForge jar: ${PF_JAR}"
echo "Using ${JAVA}"

nohup "${JAVA}" ${JAVA_OPTS} -jar "${PF_JAR}" > /dev/null 2>&1 &

echo "ProjectForge started (pid $!), see logs/ProjectForge.log in ProjectForge's home directory."
