package org.projectforge.microservices.services;

import org.projectforge.microservices.services.registration.RegistrationServer;
import org.springframework.boot.SpringApplication;

/**
 * Allow the servers to be invoke from the command-line. The jar is built with this as the <code>Main-Class</code> in
 * the jar's <code>MANIFEST.MF</code>.
 * 
 * @author Florian Blumenstein
 */
public class Main
{

  private final static String SERVER_NAME = "registration";

  public static void main(String[] args)
  {

    switch (args.length) {
      case 0:
        break;
      case 1:
        // Optionally set the HTTP port to listen on, overrides
        // value in the <server-name>-server.yml file
        System.setProperty("server.port", args[0]);
        break;
      default:
        usage();
        return;
    }

    // Tell server to look for <server-name>-server.properties or
    // <server-name>-server.yml (this app. uses YAML)
    System.setProperty("spring.config.name", SERVER_NAME + "-server");
    SpringApplication.run(RegistrationServer.class, args);

  }

  protected static void usage()
  {
    System.out.println("Usage: java -jar ... [server-port]");
    System.out.println("     where server-port > 1024");
  }
}
