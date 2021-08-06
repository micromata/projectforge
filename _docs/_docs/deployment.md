---
title: Deployment
subtitle: How to deploy a new release?
author: kai
tags: [deployment]
---

1. Modify NEWS.md with updated release informations
2. Define new release variable: +
   `export PF_NEW_VERSION=7.1.3`
3. Change version number of all `pom.xml` files: +
   `find . -name pom.xml | xargs perl -pi -e 's|<version>.*</version><!-- projectforge.version -\->|<version>$ENV{PF_NEW_VERSION}</version><!-- projectforge.version -\->|g'`
4. Check git-modifications: all `pom.xml` files should have the new ProjectForge-Version.
5. `mvn clean install`
6. If all tests are finished successfully and the test of the ProjectForge-Application works for new and existing databases), proceeed:
7. Copy executable jar file from projectforge-application/target to SourceForge and tag this file as default for all platforms
8. Building and pushing docker
    a. `docker build -t micromata/projectforge:latest .`
    b. `docker tag bc0459ed7d01 micromata/projectforge:$PF_NEW_VERSION` (use image id for tagging)
    c. `docker push micromata/projectforge:PF_NEW_VERSION`
    d. `docker push micromata/projectforge:latest`
9. Merge the release branch into the master and develop branch (Git Flow: Finish release)
10. Tag master branch with version number
11. Change to develop branch
12. Create new SNAPSHOT-Release by increasing version number of all `pom.xml` files: +
   `export PF_NEW_VERSION=7.1.4-SNAPSHOT` +
   `find . -name pom.xml | xargs perl -pi -e 's|<version>.*</version><!-- projectforge.version -\->|<version>$ENV{PF_NEW_VERSION}</version><!-- projectforge.version -\->|g'`
13. Commit everything to master and develop branch and push it to Github
14. Upload the saved jar files to Github (Create release from taged version) and SourceForge (e.g. as zip (see previous versions as example)).
