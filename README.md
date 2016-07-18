# ProjectForge - project management solution

ProjectForge is a web-based solution for project management including time tracking, team calendar, gantt-charting, financial administration, issue management,
controlling and managing work-break-down-structures (e. g. together with JIRA as issue management system).

Full documentation on: http://www.projectforge.org/pf-en/Documentation
Please visit: https://www.projectforge.org/pf-en/New_Developer_Guide

## Quickstart from command line

> Please note: You only need this package for developing ProjectForge.
> The other modules projectforge-* are automatically available by maven.

1. Checkout:  
   https://github.com/micromata/projectforge.git

2. Build ProjectForge:  
   ```mvn clean install -DskipTests=true```
   
3. Build ProjectForge-Application:  
   ```cd projectforge-application```
   ```mvn clean install -DskipTests=true```

4. Switch to compiled Jar file
   ```cd projectforge-application/target```

5. Run ProjectForge:  
   ```java -jar projectforge-application-X.X.X.jar```

6. Open your browser:  
   http://localhost:8080
   
## Quickstart with launcher

> Please note: You only need this package for developing ProjectForge.
> The other modules projectforge-* are automatically available by maven.

1. Checkout:  
   https://github.com/micromata/projectforge.git

2. Build ProjectForge:  
   ```mvn clean install -DskipTests=true```

3. Switch to compiled Jar file
   ```cd projectforge-launcher/target```

4. Run ProjectForge:  
   ```double click projectforge-launcher-X.X.X.jar```

5. Open your browser:  
   ```Start Server. Open Browser```


## Quickstart with Eclipse and maven

1. Launch eclipse

2. Import Maven project -> pom.xml in projectforge git root dir

3. Import Maven project -> pom.xml in projectforge-application dir

3. Start by simply running main (in projectforge-application/src/main/java):  
   org.projectforge.start.ProjectForgeApplication.java

Please note the detailed documentations for administrators, developers as well as for users.

Java version 8 is required since ProjectForge 6.0
Please note, that the Java version 8 is needed for the development and the running of ProjectForge. There is no delivery for Java 1.5 planned because some third party libraries are only available for Java 1.6.

## Adding your own plugins
ProjectForge support plugins. The existing menu can be modified and own entities and functionalities can be added.

The menu is customizable (you can add or remove menu entries in the config.xml file).
Deploy your plugins by adding your(r) jar(s) to the plugin directory next to the jar file. In eclipse you have to add the plugin project to the run configuration classpath. The jars contains both, the Java classes and the web pages (Wicket-pages). Nothing more is needed.
Register your plugins in the administration menu at the web gui. You need to restart the server.
One advantage is that your own plugins are independent from new releases of the ProjectForge core system. In one of the next releases an example plugin will show you how easy it is to extend ProjectForge!

