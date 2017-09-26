package org.projectforge.rest.config;

import org.glassfish.jersey.servlet.ServletContainer;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProjectforgeRestConfiguration
{
  @Bean
  public ServletRegistrationBean publicJersey()
  {
    ServletRegistrationBean publicJersey
        = new ServletRegistrationBean(new ServletContainer(new RestPublicConfiguration()));
    publicJersey.addUrlMappings("/publicRest/*");
    publicJersey.setName("RestPublic");
    publicJersey.setLoadOnStartup(0);
    return publicJersey;
  }

  @Bean
  public ServletRegistrationBean privateJersey()
  {
    ServletRegistrationBean privateJersey
        = new ServletRegistrationBean(new ServletContainer(new RestPrivateConfiguration()));
    privateJersey.addUrlMappings("/rest/*");
    privateJersey.setName("RestPrivate");
    privateJersey.setLoadOnStartup(1);
    return privateJersey;
  }

}
