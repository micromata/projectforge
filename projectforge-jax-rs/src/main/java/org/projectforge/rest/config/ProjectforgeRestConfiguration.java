package org.projectforge.rest.config;

import org.glassfish.jersey.servlet.ServletContainer;
import org.projectforge.model.rest.RestPaths;
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
    publicJersey.addUrlMappings("/" + RestPaths.PUBLIC_REST + "/*");
    publicJersey.setName("RestPublic");
    publicJersey.setLoadOnStartup(0);
    return publicJersey;
  }

  @Bean
  public ServletRegistrationBean privateJersey()
  {
    ServletRegistrationBean privateJersey
        = new ServletRegistrationBean(new ServletContainer(new RestPrivateConfiguration()));
    privateJersey.addUrlMappings("/" + RestPaths.REST + "/*");
    privateJersey.setName("RestPrivate");
    privateJersey.setLoadOnStartup(0);
    return privateJersey;
  }

}
