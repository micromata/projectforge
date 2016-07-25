package org.projectforge.microservices.address;

import java.util.logging.Logger;

import org.projectforge.microservices.services.server.AddressServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.orm.jpa.EntityScan;
import org.springframework.context.annotation.ComponentScan;

/**
 * The accounts web-application. This class has two uses:
 * <ol>
 * <li>Provide configuration and setup for {@link AddressServer} ... or</li>
 * <li>Run as a stand-alone Spring Boot web-application for testing (in which case there is <i>no</i> microservice
 * registration</li>
 * </ol>
 * <p>
 * To execute as a microservice, run {@link AddressServer} instead.
 * 
 * @author Paul Chapman
 */
@SpringBootApplication
@ComponentScan("org.projectforge")
@EntityScan("org.projectforge.jpa.model")
public class AddressWebApplication
{

  protected Logger logger = Logger.getLogger(AddressWebApplication.class
      .getName());

  /**
   * Run the application using Spring Boot and an embedded servlet engine.
   * 
   * @param args Program arguments - ignored.
   */
  public static void main(String[] args)
  {
    SpringApplication.run(AddressWebApplication.class, args);
  }

}
