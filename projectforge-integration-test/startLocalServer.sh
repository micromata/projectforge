#!/bin/bash
rm -r /Users/mhesse/ProjectForge
rm -r /Users/mhesse/Projectforge
cd /Users/mhesse/ProjectForgeWorkspace/projectforge/projectforge-application
nohup java -jar target/projectforge-application-6.2.0-SNAPSHOT.jar &