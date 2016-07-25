package org.projectforge.microservices.services.server;

import org.projectforge.microservices.address.AddressWebApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Import;

/**
 * Run as a micro-service, registering with the Discovery Server (Eureka).
 * <p>
 * Note that the configuration for this application is imported from {@link AddressWebApplication}. This is a deliberate
 * separation of concerns and allows the application to run:
 * <ul>
 * <li>Standalone - by executing {@link AddressWebApplication#main(String[])}</li>
 * <li>As a microservice - by executing {@link AddressServer#main(String[])}</li>
 * </ul>
 * 
 * @author Paul Chapman
 */
@EnableAutoConfiguration
@EnableDiscoveryClient
@Import(AddressWebApplication.class)
public class AddressServer
{
  /**
   * Run the application using Spring Boot and an embedded servlet engine.
   * 
   * @param args Program arguments - ignored.
   */
  public static void main(String[] args)
  {
    // Tell server to look for accounts-server.properties or
    // accounts-server.yml
    System.setProperty("spring.config.name", "address-server");

    SpringApplication.run(AddressServer.class, args);
  }

}
