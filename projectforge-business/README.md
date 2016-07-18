# ProjectForge - project management solution

ProjectForge is a web-based solution for project management including time tracking, team calendar, gantt-charting, financial administration, issue management,
controlling and managing work-break-down-structures (e. g. together with JIRA as issue management system).

Full documentation on: http://www.projectforge.org/pf-en/Documentation
Please visit: http://www.projectforge.org/pf-en/Developersarewelcome

## Quickstart from command line

> Please note: You only need this package for developing ProjectForge.
> The other modules projectforge-* are automatically available by maven.

1. Checkout:  
   https://github.com/micromata/projectforge-webapp.git

2. Set the JVM memory in MAVEN_OPTS or JAVA_OPTS:  
   ```-Xmx1024m -Xms512m -XX:PermSize=96m -XX:MaxPermSize=192m```

3. Build ProjectForge:  
   ```mvn -DskipTests=true install```

4. Run ProjectForge:  
   ```mvn exec:java -Dexec.mainClass="org.projectforge.web.MyStart" -Dexec.classpathScope=test```

5. Your browser will be opened after start-up automatically:  
   http://localhost:8080/ProjectForge

You may build the war for your own deployement:  
```mvn -DskipTests=true install```



## Quickstart with Eclipse and maven

1. Configure your Eclipse environment by simply typing into the command line:  
   ```mvn eclipse:eclipse```

2. Set the JVM memory in eclipse.ini or JAVA_OPTS:  
   ```-Xmx1024m -Xms512m -XX:PermSize=96m -XX:MaxPermSize=192m```

3. Launch Eclipse and open the project.

4. Add variable M2_REPO: Eclipse -> Preferences -> Java -> Build Path -> Classpath: M2_REPO=/home/kai/.m2/repository

5. Start by simply running main (in src/test/java):  
org.projectforge.web.MyStart  
   Add run configuration vm parameters (right mouse click) before starting:  
     ```-Xmx1024m -Xms512m -XX:PermSize=96m -XX:MaxPermSize=192m``` 

Please note the detailed documentations for administrators, developers as well as for users.

Java version 1.6 is required since ProjectForge 3.4
Please note, that the Java version 1.6 is needed for the development and the running of ProjectForge. There is no delivery for Java 1.5 planned because some third party libraries are only available for Java 1.6.

## Adding your own plugins
ProjectForge support plugins. The existing menu can be modified and own entities and functionalities can be added.

The menu is customizable (you can add or remove menu entries in the config.xml file).
Deploy your plugins by adding your(r) jar(s) to the WEB-INF/lib directory. The jars contains both, the Java classes and the web pages (Wicket-pages). Nothing more is needed.
Register your plugins in the config.xml file.
One advantage is that your own plugins are independent from new releases of the ProjectForge core system. In one of the next releases an example plugin will show you how easy it is to extend ProjectForge!

