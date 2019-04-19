package org.projectforge.rest.config;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.projectforge.model.rest.RestPaths;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProjectForgeRestConfiguration {
  public static final String REST_WEB_APP_URL = "/" + RestPaths.REST_WEB_APP + "/";
  public static final String REST_WEB_APP_PUBLIC_URL = "/" + RestPaths.REST_WEB_APP_PUBLIC + "/";

  @Bean
  public ServletRegistrationBean publicJersey() {
    ServletRegistrationBean publicJersey
            = new ServletRegistrationBean(new ServletContainer(new RestPublicConfiguration()));
    publicJersey.addUrlMappings("/" + RestPaths.PUBLIC_REST + "/*");
    publicJersey.setName("RestPublic");
    publicJersey.setLoadOnStartup(0);
    return publicJersey;
  }

  @Bean
  public ServletRegistrationBean privateJersey() {
    ServletRegistrationBean privateJersey
            = new ServletRegistrationBean(new ServletContainer(new RestPrivateConfiguration()));
    privateJersey.addUrlMappings("/" + RestPaths.REST + "/*");
    privateJersey.setName("RestPrivate");
    privateJersey.setLoadOnStartup(0);
    return privateJersey;
  }

  @Bean
  public ServletRegistrationBean webAppJersey() {
    ResourceConfig resourceConfig = new RestWebAppConfiguration();
    resourceConfig.register(ObjectMapperResolver.class);
    ServletContainer container = new ServletContainer(resourceConfig);
    ServletRegistrationBean webAppJersey = new ServletRegistrationBean(container);
    webAppJersey.addUrlMappings(REST_WEB_APP_URL + "*");
    webAppJersey.setName("RestWebapp");
    webAppJersey.setLoadOnStartup(0);
    return webAppJersey;
  }

  @Bean
  public ServletRegistrationBean webAppPublicJersey() {
    ResourceConfig resourceConfig = new RestWebAppPublicConfiguration();
    resourceConfig.register(ObjectMapperResolver.class);
    ServletContainer container = new ServletContainer(resourceConfig);
    ServletRegistrationBean webAppJersey = new ServletRegistrationBean(container);
    webAppJersey.addUrlMappings(REST_WEB_APP_PUBLIC_URL + "*");
    webAppJersey.setName("RestWebappPublic");
    webAppJersey.setLoadOnStartup(0);
    return webAppJersey;
  }
}
